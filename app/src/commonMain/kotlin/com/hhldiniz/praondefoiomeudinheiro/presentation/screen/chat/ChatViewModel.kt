package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.chat.AssistantPrompt
import com.hhldiniz.praondefoiomeudinheiro.data.chat.ChatModelManager
import com.hhldiniz.praondefoiomeudinheiro.data.chat.ChatPromptBuilder
import com.hhldiniz.praondefoiomeudinheiro.data.chat.FinancialContextBuilder
import com.hhldiniz.praondefoiomeudinheiro.data.chat.FinancialSnapshot
import com.hhldiniz.praondefoiomeudinheiro.data.chat.ModelFailure
import com.hhldiniz.praondefoiomeudinheiro.data.chat.ModelStatus
import com.hhldiniz.praondefoiomeudinheiro.data.chat.StopSequenceFilter
import com.hhldiniz.praondefoiomeudinheiro.data.local.CurrencyHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.PatrimonyHolder
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.InvestmentRepository
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatMessage
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatRole
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.GenerationOptions
import com.hhldiniz.praondefoiomeudinheiro.domain.model.UiText
import com.hhldiniz.praondefoiomeudinheiro.platform.currentTimeMillis
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_error_empty_answer
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_error_generation
import com.hhldiniz.praondefoiomeudinheiro.util.formatDayMonthYear
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.hhldiniz.praondefoiomeudinheiro.platform.ioDispatcher as platformIoDispatcher

/**
 * The assistant chat: one conversation with the on-device model, about the
 * user's own money.
 *
 * The model's life (download, load) belongs to [ChatModelManager], which
 * outlives this screen; this ViewModel holds the conversation, and before
 * every answer re-reads the user's data into a fresh briefing
 * ([FinancialContextBuilder]) so an entry imported a minute ago is already
 * part of what the assistant knows.
 *
 * Opening the screen loads an already-downloaded model on its own; leaving it
 * frees that memory again. The download itself always waits for the user to
 * ask for it — it is a gigabyte.
 */
class ChatViewModel(
    private val manager: ChatModelManager,
    private val importRepository: ImportRepository,
    private val investmentRepository: InvestmentRepository,
    private val ioDispatcher: CoroutineDispatcher = platformIoDispatcher,
) : ViewModel() {

    private val promptBuilder = ChatPromptBuilder(manager.model.promptFormat)
    private val options = GenerationOptions(maxTokens = MAX_ANSWER_TOKENS)

    private val _uiState = MutableStateFlow(ChatUiState(model = manager.model, status = manager.status.value))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var languageTag: String = "pt"
    private var nextId = 0L
    private var generation: Job? = null

    init {
        viewModelScope.launch {
            manager.status.collect { status ->
                _uiState.update { it.copy(status = status) }
                // Load as soon as the file is there — on opening the screen,
                // or the moment a download started here finishes.
                if (status is ModelStatus.Downloaded) manager.load()
            }
        }
        manager.refresh()
    }

    /** The app's language, used when the user's message is too short to tell which one to answer in. */
    fun onLanguage(tag: String) {
        languageTag = tag
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    fun onDownload() = manager.startDownload()

    fun onCancelDownload() = manager.cancelDownload()

    /** Retries whatever failed: the download, or loading the file into memory. */
    fun onRetry() {
        when (val status = _uiState.value.status) {
            is ModelStatus.Failed -> when (status.failure) {
                ModelFailure.LOAD, ModelFailure.CRASHED -> manager.load()
                else -> manager.startDownload()
            }
            else -> Unit
        }
    }

    /** Removes the model from the device. The conversation goes with it, since it can no longer continue. */
    fun onDeleteModel() {
        onStop()
        _uiState.update { it.copy(messages = emptyList(), error = null) }
        manager.delete()
    }

    /** Starts over: a new conversation, with the model still loaded. */
    fun onNewConversation() {
        onStop()
        _uiState.update { it.copy(messages = emptyList(), error = null, input = "") }
    }

    /** Sends a suggested question as if it had been typed. */
    fun onSuggestion(text: String) {
        _uiState.update { it.copy(input = text) }
        send()
    }

    fun send() {
        val state = _uiState.value
        if (!state.canSend) return
        val question = state.input.trim()

        val history = state.messages
            .filter { it.text.isNotBlank() }
            .map { ChatMessage(it.role, it.text) }
        val userBubble = ChatBubble(nextId++, ChatRole.USER, question)
        val answerId = nextId++
        _uiState.update {
            it.copy(
                messages = it.messages + userBubble + ChatBubble(answerId, ChatRole.ASSISTANT, "", isStreaming = true),
                input = "",
                isGenerating = true,
                isThinking = true,
                error = null,
            )
        }

        generation = viewModelScope.launch {
            var failed = false
            var stopped = false
            try {
                val prompt = withContext(ioDispatcher) {
                    val system = AssistantPrompt.build(
                        financialData = FinancialContextBuilder.describe(loadSnapshot()),
                        today = formatDayMonthYear(currentTimeMillis()),
                        languageTag = languageTag,
                    )
                    promptBuilder.build(
                        system = system,
                        conversation = history + ChatMessage(ChatRole.USER, question),
                        maxPromptTokens = manager.model.contextLength - MAX_ANSWER_TOKENS - CONTEXT_MARGIN_TOKENS,
                    )
                }

                val filter = StopSequenceFilter(promptBuilder.stopSequences + options.stop)
                manager.generate(prompt, options)
                    .transformWhile { piece ->
                        val visible = filter.push(piece)
                        if (visible.isNotEmpty()) emit(visible)
                        !filter.isStopped
                    }
                    .collect { visible -> appendToAnswer(answerId, visible) }
                appendToAnswer(answerId, filter.flush())
            } catch (cancelled: CancellationException) {
                // Stopped by the user: what was written so far stays on screen.
                stopped = true
                throw cancelled
            } catch (error: Exception) {
                failed = true
                _uiState.update { it.copy(error = UiText.Localized(Res.string.chat_error_generation)) }
            } finally {
                finishAnswer(answerId, failed, stopped)
            }
        }
    }

    /** Stops the answer being written; the part already shown is kept. */
    fun onStop() {
        generation?.cancel()
        generation = null
    }

    private suspend fun loadSnapshot(): FinancialSnapshot = FinancialSnapshot(
        currency = CurrencyHolder.selectedCurrency.value,
        patrimony = PatrimonyHolder.patrimony.value,
        entries = importRepository.getAllEntries(),
        investments = investmentRepository.getAll().first(),
    )

    private fun appendToAnswer(id: Long, text: String) {
        if (text.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                isThinking = false,
                messages = state.messages.map { bubble ->
                    if (bubble.id != id) {
                        bubble
                    } else {
                        // The model tends to open with a newline or a space.
                        val combined = bubble.text + text
                        bubble.copy(text = if (bubble.text.isEmpty()) combined.trimStart() else combined)
                    }
                },
            )
        }
    }

    private fun finishAnswer(id: Long, failed: Boolean, stopped: Boolean) {
        _uiState.update { state ->
            val answer = state.messages.firstOrNull { it.id == id }
            val emptyAnswer = answer != null && answer.text.isBlank()
            state.copy(
                isGenerating = false,
                isThinking = false,
                // An answer with no text is no answer: drop the empty bubble
                // and say so, unless the error already did.
                messages = if (emptyAnswer) {
                    state.messages.filterNot { it.id == id }
                } else {
                    state.messages.map { if (it.id == id) it.copy(isStreaming = false, text = it.text.trimEnd(), failed = failed) else it }
                },
                error = state.error ?: if (emptyAnswer && !failed && !stopped) {
                    UiText.Localized(Res.string.chat_error_empty_answer)
                } else {
                    null
                },
            )
        }
    }

    override fun onCleared() {
        generation?.cancel()
        // The weights take over a gigabyte of memory; nothing else in the app
        // needs them, so they go when the chat does. The file stays.
        manager.unload()
    }

    companion object {
        /** Room left for the answer inside the context window. */
        const val MAX_ANSWER_TOKENS = 512

        /** Slack for the gap between the prompt's token estimate and the real tokenizer. */
        private const val CONTEXT_MARGIN_TOKENS = 128
    }
}
