package com.hhldiniz.praondefoiomeudinheiro.data.chat

import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatModel
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.DownloadProgress
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.GenerationOptions
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.LlmRuntime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * An [LlmRuntime] with no model behind it: it "answers" with [answer], split
 * into the pieces given, and records every prompt it was handed — which is
 * how the tests check what the assistant was actually asked.
 */
class FakeLlmRuntime(
    var downloaded: Boolean = false,
) : LlmRuntime {

    var answer: List<String> = listOf("Ok.")
    var downloadError: Exception? = null
    var loadError: Exception? = null
    var generationError: Exception? = null

    /** When set, [download] waits on it, so a test can look at (or cancel) a download in flight. */
    var downloadGate: CompletableDeferred<Unit>? = null

    /** When set, [load] waits on it — a load that never finishes, as a crashed engine's would. */
    var loadGate: CompletableDeferred<Unit>? = null

    val prompts = mutableListOf<String>()
    var loads = 0
        private set
    var unloads = 0
        private set
    var deletes = 0
        private set

    override suspend fun isDownloaded(model: ChatModel): Boolean = downloaded

    override suspend fun download(model: ChatModel, onProgress: (DownloadProgress) -> Unit) {
        onProgress(DownloadProgress(model.sizeBytes / 4, model.sizeBytes))
        downloadGate?.await()
        downloadError?.let { throw it }
        onProgress(DownloadProgress(model.sizeBytes, model.sizeBytes))
        downloaded = true
    }

    override suspend fun delete(model: ChatModel) {
        deletes++
        downloaded = false
    }

    override suspend fun load(model: ChatModel) {
        loadGate?.await()
        loadError?.let { throw it }
        loads++
    }

    override suspend fun unload() {
        unloads++
    }

    override fun complete(prompt: String, options: GenerationOptions): Flow<String> = flow {
        prompts += prompt
        for (piece in answer) emit(piece)
        generationError?.let { throw it }
    }
}
