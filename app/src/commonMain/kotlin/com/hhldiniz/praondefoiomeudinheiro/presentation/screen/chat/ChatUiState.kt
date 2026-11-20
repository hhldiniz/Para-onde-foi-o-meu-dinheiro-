package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.chat

import com.hhldiniz.praondefoiomeudinheiro.data.chat.ModelStatus
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatModel
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatRole
import com.hhldiniz.praondefoiomeudinheiro.domain.model.UiText

/** One message on screen. [isStreaming] is true while the assistant is still writing it. */
data class ChatBubble(
    val id: Long,
    val role: ChatRole,
    val text: String,
    val isStreaming: Boolean = false,
    /** Set when the answer was cut short by an error; the partial text is kept. */
    val failed: Boolean = false,
)

data class ChatUiState(
    val model: ChatModel,
    val status: ModelStatus = ModelStatus.Checking,
    val messages: List<ChatBubble> = emptyList(),
    val input: String = "",
    val isGenerating: Boolean = false,
    /** Set while the user's data is being summarized and the prompt read, before the first word. */
    val isThinking: Boolean = false,
    val error: UiText? = null,
) {
    val canSend: Boolean
        get() = status is ModelStatus.Ready && !isGenerating && input.isNotBlank()
}
