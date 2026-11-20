package com.hhldiniz.praondefoiomeudinheiro.data.chat

import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatMessage
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatRole
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.PromptFormat

/**
 * Renders a conversation into the single prompt string a model continues.
 *
 * The app writes the chat template itself rather than leaving it to the
 * model's embedded Jinja template: Llamatik and wllama would otherwise each
 * apply it their own way, and this way the exact text the model sees is the
 * same on every platform and pinned by `ChatPromptBuilderTest`.
 *
 * It also keeps the prompt inside the model's context window. A small model's
 * window is a few thousand tokens and the answer needs room too, so older
 * turns are dropped — oldest first, never the system prompt and never the
 * question being asked — until the estimate fits.
 */
class ChatPromptBuilder(private val format: PromptFormat) {

    /** Text that marks the end of the assistant's turn in this format. */
    val stopSequences: List<String>
        get() = when (format) {
            PromptFormat.CHATML -> listOf(CHATML_END, CHATML_START)
        }

    /**
     * Builds the prompt for the model's next answer.
     *
     * @param system instructions and context for the whole conversation.
     * @param conversation the turns so far, oldest first; the last one must be
     *   the user's question.
     * @param maxPromptTokens how many tokens the prompt may use, i.e. the
     *   context window minus what is reserved for the answer.
     */
    fun build(system: String, conversation: List<ChatMessage>, maxPromptTokens: Int): String {
        require(conversation.isNotEmpty() && conversation.last().role == ChatRole.USER) {
            "the conversation has to end with the user's question"
        }
        val turns = conversation.filter { it.role != ChatRole.SYSTEM && it.content.isNotBlank() }
            .map { it.copy(content = sanitize(it.content)) }

        // The system prompt may take at most half of the budget, which leaves
        // the question and at least some history room to fit.
        var systemText = sanitize(system)
        val systemBudget = maxPromptTokens / 2
        if (estimateTokens(systemText) > systemBudget) {
            systemText = systemText.take(systemBudget * CHARS_PER_TOKEN).trimEnd() + "\n…"
        }

        val question = turns.last()
        var remaining = maxPromptTokens - estimateTokens(render(systemText, emptyList())) - turnCost(question)

        // Walk back from the most recent turn, keeping each while it fits.
        val kept = ArrayDeque<ChatMessage>()
        for (turn in turns.dropLast(1).asReversed()) {
            val cost = turnCost(turn)
            if (cost > remaining) break
            remaining -= cost
            kept.addFirst(turn)
        }
        // An assistant turn with no question before it reads as the model
        // talking to itself; start the window on a user turn.
        while (kept.firstOrNull()?.role == ChatRole.ASSISTANT) kept.removeFirst()

        val asked = if (remaining < 0) {
            // The question alone does not fit: keep its end, which is usually
            // where the actual question is.
            val allowed = (turnCost(question) + remaining).coerceAtLeast(MIN_QUESTION_TOKENS)
            question.copy(content = "…" + question.content.takeLast(allowed * CHARS_PER_TOKEN))
        } else {
            question
        }
        return render(systemText, kept + asked)
    }

    private fun render(system: String, turns: List<ChatMessage>): String = when (format) {
        PromptFormat.CHATML -> buildString {
            append(CHATML_START).append("system\n").append(system).append(CHATML_END).append('\n')
            for (turn in turns) {
                append(CHATML_START).append(roleName(turn.role)).append('\n')
                append(turn.content).append(CHATML_END).append('\n')
            }
            append(CHATML_START).append("assistant\n")
        }
    }

    private fun turnCost(turn: ChatMessage): Int = estimateTokens(turn.content) + TURN_OVERHEAD_TOKENS

    private fun roleName(role: ChatRole): String = when (role) {
        ChatRole.SYSTEM -> "system"
        ChatRole.USER -> "user"
        ChatRole.ASSISTANT -> "assistant"
    }

    companion object {
        private const val CHATML_START = "<|im_start|>"
        private const val CHATML_END = "<|im_end|>"

        /**
         * Characters per token used for the budget. Real tokenizers average
         * closer to 4 for Portuguese and English prose, but numbers, dates and
         * amounts — most of what this app puts in a prompt — tokenize far
         * worse, so the estimate errs on the side of fewer characters.
         */
        private const val CHARS_PER_TOKEN = 3

        /** The role header and end marker around each turn. */
        private const val TURN_OVERHEAD_TOKENS = 5

        private const val MIN_QUESTION_TOKENS = 64

        /** Rough token count of [text]; see [CHARS_PER_TOKEN]. */
        fun estimateTokens(text: String): Int = (text.length + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN

        /**
         * Removes the template's own control markers from user-supplied text.
         * llama.cpp parses special tokens inside the prompt, so a message (or
         * a transaction description) containing `<|im_end|>` would otherwise
         * end its turn early and inject a turn of its own.
         */
        fun sanitize(text: String): String =
            text.replace(CHATML_START, "").replace(CHATML_END, "").replace("<|endoftext|>", "")
    }
}

/**
 * Cuts a streamed answer at the first stop sequence, even when the sequence
 * arrives split across pieces (`"<|im"` then `"_end|>"`). Text that might be
 * the beginning of a stop sequence is held back until the next piece shows
 * whether it is.
 */
class StopSequenceFilter(private val stops: List<String>) {

    private val pending = StringBuilder()

    /** Whether a stop sequence has been seen; everything after it is discarded. */
    var isStopped: Boolean = false
        private set

    /** Adds [piece] and returns the part of the answer that is now safe to show. */
    fun push(piece: String): String {
        if (isStopped) return ""
        pending.append(piece)

        val stopAt = stops.mapNotNull { stop -> pending.indexOf(stop).takeIf { it >= 0 } }.minOrNull()
        if (stopAt != null) {
            isStopped = true
            val visible = pending.substring(0, stopAt)
            pending.clear()
            return visible
        }

        val held = heldBackLength()
        val visible = pending.substring(0, pending.length - held)
        pending.deleteRange(0, pending.length - held)
        return visible
    }

    /** Returns whatever was held back, once the stream has ended without a stop sequence. */
    fun flush(): String {
        if (isStopped) return ""
        val rest = pending.toString()
        pending.clear()
        return rest
    }

    /** Length of the longest tail of [pending] that is the start of some stop sequence. */
    private fun heldBackLength(): Int {
        var longest = 0
        for (stop in stops) {
            val max = minOf(stop.length - 1, pending.length)
            for (length in max downTo longest + 1) {
                if (pending.endsWith(stop.substring(0, length))) {
                    longest = length
                    break
                }
            }
        }
        return longest
    }

    private fun StringBuilder.endsWith(suffix: String): Boolean =
        length >= suffix.length && substring(length - suffix.length) == suffix
}
