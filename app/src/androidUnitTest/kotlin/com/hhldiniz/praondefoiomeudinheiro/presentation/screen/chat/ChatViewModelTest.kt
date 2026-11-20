package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.chat

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hhldiniz.praondefoiomeudinheiro.data.chat.ChatModelManager
import com.hhldiniz.praondefoiomeudinheiro.data.chat.FakeLlmRuntime
import com.hhldiniz.praondefoiomeudinheiro.data.chat.ModelStatus
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.ImportedEntryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.InvestmentDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.InvestmentRepository
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatModels
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatRole
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.LlmRuntimeException
import com.hhldiniz.praondefoiomeudinheiro.domain.model.UiText
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_error_empty_answer
import com.hhldiniz.praondefoiomeudinheiro.resources.chat_error_generation
import com.hhldiniz.praondefoiomeudinheiro.util.startOfDayMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * [ChatViewModel] over a [FakeLlmRuntime]: no model is involved, so what is
 * checked is what the assistant gets asked (the prompt the fake records) and
 * what the user gets to see.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val dispatcher = UnconfinedTestDispatcher()

    private val entries = listOf(
        ImportedEntry(
            dateMillis = startOfDayMillis(2026, 10, 3),
            amount = 812.0,
            description = "Aluguel",
            category = "Moradia",
            isExpense = true,
            importedAt = 0L,
        ),
    )

    private lateinit var runtime: FakeLlmRuntime
    private lateinit var manager: ChatModelManager

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        runtime = FakeLlmRuntime(downloaded = true)
        manager = ChatModelManager(runtime, ChatModels.DEFAULT, CoroutineScope(dispatcher))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): ChatViewModel {
        val entryDao = mock<ImportedEntryDao> { onBlocking { getAllEntriesByDate() } doReturn entries }
        val investmentDao = mock<InvestmentDao> { on { getAll() } doReturn flowOf(emptyList()) }
        return ChatViewModel(manager, ImportRepository(entryDao), InvestmentRepository(investmentDao), dispatcher)
    }

    private fun ChatViewModel.ask(question: String) {
        onInputChanged(question)
        send()
    }

    @Test
    fun `opening the chat loads a model that is already on the device`() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertEquals(ModelStatus.Ready, viewModel.uiState.value.status)
        assertEquals(1, runtime.loads)
    }

    @Test
    fun `the download waits for the user, then the model loads by itself`() = runTest(dispatcher) {
        runtime.downloaded = false
        val viewModel = viewModel()
        assertEquals(ModelStatus.NotDownloaded, viewModel.uiState.value.status)
        assertFalse(viewModel.uiState.value.canSend)

        viewModel.onDownload()

        assertEquals(ModelStatus.Ready, viewModel.uiState.value.status)
    }

    @Test
    fun `an answer is streamed and cut at the end-of-turn marker`() = runTest(dispatcher) {
        runtime.answer = listOf("\nVocê gastou", " mais com Moradia.<|im", "_end|><|im_start|>user")
        val viewModel = viewModel()

        viewModel.ask("Onde gastei mais?")

        val messages = viewModel.uiState.value.messages
        assertEquals(listOf(ChatRole.USER, ChatRole.ASSISTANT), messages.map { it.role })
        assertEquals("Onde gastei mais?", messages[0].text)
        assertEquals("Você gastou mais com Moradia.", messages[1].text)
        assertFalse(messages[1].isStreaming)
        assertFalse(viewModel.uiState.value.isGenerating)
        assertEquals("", viewModel.uiState.value.input)
    }

    @Test
    fun `the prompt carries the user's data and the conversation so far`() = runTest(dispatcher) {
        runtime.answer = listOf("Moradia.")
        val viewModel = viewModel()

        viewModel.ask("Onde gastei mais?")
        viewModel.ask("E quanto foi?")

        val second = runtime.prompts.last()
        assertTrue(second.contains("USER'S FINANCIAL DATA"))
        assertTrue(second.contains("- Moradia: R$ 812.00 (100%)"))
        assertTrue(
            second.contains(
                "<|im_start|>user\nOnde gastei mais?<|im_end|>\n" +
                    "<|im_start|>assistant\nMoradia.<|im_end|>\n" +
                    "<|im_start|>user\nE quanto foi?<|im_end|>\n<|im_start|>assistant\n"
            )
        )
    }

    @Test
    fun `a failed answer keeps what was written and says so`() = runTest(dispatcher) {
        runtime.answer = listOf("Você gastou")
        runtime.generationError = LlmRuntimeException("decode failed")
        val viewModel = viewModel()

        viewModel.ask("Onde gastei mais?")

        val state = viewModel.uiState.value
        assertEquals("Você gastou", state.messages.last().text)
        assertTrue(state.messages.last().failed)
        assertEquals(UiText.Localized(Res.string.chat_error_generation), state.error)
    }

    @Test
    fun `an empty answer is dropped instead of shown as a blank bubble`() = runTest(dispatcher) {
        runtime.answer = listOf("  ", "<|im_end|>")
        val viewModel = viewModel()

        viewModel.ask("Oi")

        val state = viewModel.uiState.value
        assertEquals(listOf(ChatRole.USER), state.messages.map { it.role })
        assertEquals(UiText.Localized(Res.string.chat_error_empty_answer), state.error)
    }

    @Test
    fun `nothing is sent while the model is not ready or the input is blank`() = runTest(dispatcher) {
        runtime.downloaded = false
        val viewModel = viewModel()

        viewModel.ask("Onde gastei mais?")
        assertTrue(viewModel.uiState.value.messages.isEmpty())

        viewModel.onDownload()
        viewModel.ask("   ")
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertTrue(runtime.prompts.isEmpty())
    }

    @Test
    fun `a suggestion is sent as a question`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onSuggestion("Resuma meus investimentos.")

        assertEquals("Resuma meus investimentos.", viewModel.uiState.value.messages.first().text)
        assertEquals(1, runtime.prompts.size)
    }

    @Test
    fun `a new conversation starts empty and deleting the model clears it too`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.ask("Oi")

        viewModel.onNewConversation()
        assertTrue(viewModel.uiState.value.messages.isEmpty())

        viewModel.ask("Oi de novo")
        viewModel.onDeleteModel()
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertEquals(ModelStatus.NotDownloaded, viewModel.uiState.value.status)
        assertEquals(1, runtime.deletes)
    }
}
