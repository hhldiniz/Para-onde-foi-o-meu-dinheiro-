package com.hhldiniz.praondefoiomeudinheiro.domain.chat

import kotlinx.coroutines.flow.Flow

/** Who said a [ChatMessage]. */
enum class ChatRole { SYSTEM, USER, ASSISTANT }

/** One turn of a conversation, as it is handed to the model. */
data class ChatMessage(
    val role: ChatRole,
    val content: String,
)

/** Sampling settings for one answer. */
data class GenerationOptions(
    val maxTokens: Int = 768,
    val temperature: Float = 0.6f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
    /**
     * Text that ends the answer. The runtimes stop on the model's own
     * end-of-turn token already; these are the textual safety net for a
     * runtime that lets one through as text.
     */
    val stop: List<String> = emptyList(),
)

/** How far along a model download is. [totalBytes] is 0 while unknown. */
data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
) {
    val fraction: Float
        get() = if (totalBytes <= 0L) 0f else (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
}

/** A failure the runtime reports in its own words, e.g. llama.cpp refusing a file. */
class LlmRuntimeException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * The on-device inference engine behind the assistant chat — the one part of
 * it that differs per platform. Everything that decides *what* to ask the
 * model (the prompt, the user's financial summary, the conversation's token
 * budget) lives in common code; an implementation only stores the weights and
 * runs llama.cpp over a ready-made prompt:
 *
 * - Android and iOS: `LlamatikRuntime` (`llamaMain`), llama.cpp through the
 *   Llamatik library, with the GGUF file downloaded into app-private storage.
 * - Web: `WllamaRuntime` (`wasmJsMain`), llama.cpp compiled to WebAssembly
 *   (wllama, vendored like pdf.js), with the file kept in the browser's
 *   origin-private file system.
 *
 * Nothing is sent anywhere: the only network traffic is the one-time download
 * of the model from Hugging Face.
 */
interface LlmRuntime {

    /** Whether [model] is fully on this device and can be loaded without a network. */
    suspend fun isDownloaded(model: ChatModel): Boolean

    /**
     * Downloads [model], reporting progress as it goes. Suspends until the file
     * is complete; cancelling the calling coroutine stops the transfer. A
     * runtime that can resume keeps what it has so the next call continues
     * from there.
     */
    suspend fun download(model: ChatModel, onProgress: (DownloadProgress) -> Unit)

    /** Removes [model]'s file (and any partial download) from the device. */
    suspend fun delete(model: ChatModel)

    /** Loads [model]'s weights into memory. Calling it again for the loaded model does nothing. */
    suspend fun load(model: ChatModel)

    /** Frees the loaded model, if any. */
    suspend fun unload()

    /**
     * Streams the model's continuation of [prompt] — already rendered in the
     * model's chat format — piece by piece. Cancelling the collection stops the
     * generation. One generation runs at a time.
     */
    fun complete(prompt: String, options: GenerationOptions): Flow<String>
}
