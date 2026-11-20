package com.hhldiniz.praondefoiomeudinheiro.data.chat

import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatModel
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.DownloadProgress
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.GenerationOptions
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.LlmRuntime
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.LlmRuntimeException
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// `window.praOndeLlm`, defined by `llm-chat.js` (a module script in
// `index.html`), which imports the vendored wllama lazily on first use.

private fun isBridgeReady(): Boolean = js("typeof window.praOndeLlm === 'object' && window.praOndeLlm !== null")

private fun jsIsDownloaded(url: String): Promise<JsAny?> = js("window.praOndeLlm.isDownloaded(url)")

private fun jsDownload(url: String, size: Double, onProgress: (Double, Double) -> Unit): Promise<JsAny?> =
    js("window.praOndeLlm.download(url, size, onProgress)")

private fun jsCancelDownload(): Unit = js("window.praOndeLlm.cancelDownload()")

private fun jsRemove(url: String): Promise<JsAny?> = js("window.praOndeLlm.remove(url)")

private fun jsLoad(url: String, contextLength: Int): Promise<JsAny?> = js("window.praOndeLlm.load(url, contextLength)")

private fun jsUnload(): Promise<JsAny?> = js("window.praOndeLlm.unload()")

private fun jsComplete(prompt: String, optionsJson: String, onPiece: (String) -> Unit): Promise<JsAny?> =
    js("window.praOndeLlm.complete(prompt, optionsJson, onPiece)")

private fun jsAbortCompletion(): Unit = js("window.praOndeLlm.abortCompletion()")

/** See `TextRecognizer.wasmJs.kt`: the declared return type is what converts the JS string back. */
private fun jsAnyToKotlinString(value: JsAny): String = js("value")

/**
 * Awaits one of the bridge's promises, which always resolve with a string
 * (see `wrap` in `llm-chat.js`): the result, or [FAILED] and a message.
 */
private suspend fun Promise<JsAny?>.result(): String {
    val value = await<JsAny?>()?.let(::jsAnyToKotlinString).orEmpty()
    if (value.startsWith(FAILED)) throw bridgeFailure(value.removePrefix(FAILED))
    return value
}

/** Must match `FAILED` in `llm-chat.js`. */
private const val FAILED = "praonde:failed:"

/** Must match `NOT_ENOUGH_STORAGE` in `llm-chat.js`. */
private const val NOT_ENOUGH_STORAGE = "praonde:not-enough-storage"

/** Turns the bridge's storage marker back into the exception the UI knows how to explain. */
private fun bridgeFailure(message: String): Exception {
    if (message.startsWith(NOT_ENOUGH_STORAGE)) {
        val parts = message.removePrefix("$NOT_ENOUGH_STORAGE:").split(':')
        return NotEnoughStorageException(
            requiredBytes = parts.getOrNull(0)?.toDoubleOrNull()?.toLong() ?: 0L,
            availableBytes = parts.getOrNull(1)?.toDoubleOrNull()?.toLong() ?: 0L,
        )
    }
    return LlmRuntimeException(message.ifBlank { "wllama failed" })
}

/** What `complete` reads its sampling settings from, in the names the bridge expects. */
@Serializable
private class CompletionOptions(
    val maxTokens: Int,
    val temperature: Float,
    val topP: Float,
    val topK: Int,
    val repeatPenalty: Float,
    val stop: List<String>,
)

/**
 * [LlmRuntime] for the web: llama.cpp compiled to WebAssembly (wllama),
 * through `llm-chat.js`. The model is downloaded straight from Hugging Face by
 * the browser and kept in its origin-private file system, so after the first
 * visit the chat works offline like the rest of the PWA.
 *
 * A browser cannot resume a download the way the native runtimes do; an
 * interrupted one starts over.
 */
class WllamaRuntime : LlmRuntime {

    private fun requireBridge() = check(isBridgeReady()) {
        "llm-chat.js is not loaded: the assistant's bridge script did not run on this page"
    }

    override suspend fun isDownloaded(model: ChatModel): Boolean {
        requireBridge()
        return jsIsDownloaded(model.downloadUrl).result() == "true"
    }

    override suspend fun download(model: ChatModel, onProgress: (DownloadProgress) -> Unit) {
        requireBridge()
        try {
            jsDownload(model.downloadUrl, model.sizeBytes.toDouble()) { loaded, total ->
                onProgress(DownloadProgress(loaded.toLong(), total.toLong()))
            }.result()
        } catch (cancelled: CancellationException) {
            jsCancelDownload()
            throw cancelled
        }
    }

    override suspend fun delete(model: ChatModel) {
        requireBridge()
        jsRemove(model.downloadUrl).result()
    }

    override suspend fun load(model: ChatModel) {
        requireBridge()
        jsLoad(model.downloadUrl, model.contextLength).result()
    }

    override suspend fun unload() {
        if (!isBridgeReady()) return
        jsUnload().result()
    }

    override fun complete(prompt: String, options: GenerationOptions): Flow<String> = callbackFlow {
        requireBridge()
        val settings = Json.encodeToString(
            CompletionOptions(
                maxTokens = options.maxTokens,
                temperature = options.temperature,
                topP = options.topP,
                topK = options.topK,
                repeatPenalty = options.repeatPenalty,
                stop = options.stop,
            )
        )
        val finished = jsComplete(prompt, settings) { piece -> trySend(piece) }
        launch {
            try {
                finished.result()
                channel.close()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                channel.close(error)
            }
        }
        awaitClose { jsAbortCompletion() }
    }.buffer(Channel.UNLIMITED)
}
