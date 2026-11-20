package com.hhldiniz.praondefoiomeudinheiro.data.chat

import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatModel
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.DownloadProgress
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.GenerationOptions
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.LlmRuntime
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.LlmRuntimeException
import com.hhldiniz.praondefoiomeudinheiro.platform.ioDispatcher
import com.llamatik.library.platform.GenStream
import com.llamatik.library.platform.LlamaBridge
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/** Absolute path of the app-private directory the model lives in, created if missing and kept out of backups. */
internal expect fun modelDirectoryPath(): String

/** Bytes free on the volume holding [path], or null when the platform cannot tell. */
internal expect fun freeSpaceBytes(path: String): Long?

/** CPU cores available to the app. */
internal expect fun processorCount(): Int

/**
 * [LlmRuntime] for Android and iOS: llama.cpp through Llamatik, reading a
 * GGUF file downloaded into app-private storage (`noBackupFilesDir` on
 * Android, Application Support excluded from iCloud backup on iOS — a
 * gigabyte that can always be downloaded again has no business in a backup).
 *
 * The download is resumable: bytes go to `<model>.gguf.part`, an interrupted
 * transfer continues from that file's size with an HTTP range request, and
 * only a file of exactly [ChatModel.sizeBytes] is renamed into place — so
 * [isDownloaded] can never mistake a truncated file for a model.
 *
 * Llamatik's `LlamaBridge` is a process-wide singleton that blocks the calling
 * thread for loads and generations, so every call into it goes through one
 * dedicated thread ([nativeDispatcher]) and generations are serialized by
 * [generationLock].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LlamatikRuntime(
    private val client: HttpClient = HttpClient(),
) : LlmRuntime {

    private val nativeDispatcher = ioDispatcher.limitedParallelism(1)
    private val generationLock = Mutex()
    private var loadedModelId: String? = null

    /** The loaded model's context window, re-sent with every generation's sampling settings. */
    private var contextLength: Int = 4096

    private fun modelFile(model: ChatModel) = Path(modelDirectoryPath(), "${model.id}.gguf")

    private fun partialFile(model: ChatModel) = Path(modelDirectoryPath(), "${model.id}.gguf.part")

    private fun sizeOf(path: Path): Long? = SystemFileSystem.metadataOrNull(path)?.size

    override suspend fun isDownloaded(model: ChatModel): Boolean = withContext(ioDispatcher) {
        sizeOf(modelFile(model)) == model.sizeBytes
    }

    override suspend fun download(model: ChatModel, onProgress: (DownloadProgress) -> Unit) = withContext(ioDispatcher) {
        if (sizeOf(modelFile(model)) == model.sizeBytes) return@withContext
        val partial = partialFile(model)

        var resumeFrom = sizeOf(partial) ?: 0L
        if (resumeFrom >= model.sizeBytes) {
            // Anything this long that was not renamed into place is corrupt.
            SystemFileSystem.delete(partial, mustExist = false)
            resumeFrom = 0L
        }

        val missing = model.sizeBytes - resumeFrom
        freeSpaceBytes(modelDirectoryPath())?.let { free ->
            if (free < missing + STORAGE_HEADROOM_BYTES) throw NotEnoughStorageException(missing, free)
        }

        client.prepareGet(model.downloadUrl) {
            if (resumeFrom > 0) header(HttpHeaders.Range, "bytes=$resumeFrom-")
        }.execute { response ->
            val append = when (response.status) {
                HttpStatusCode.PartialContent -> true
                // A server that ignores the range sends the whole file again.
                HttpStatusCode.OK -> false
                else -> throw LlmRuntimeException("HTTP ${response.status.value} downloading ${model.fileName}")
            }
            var written = if (append) resumeFrom else 0L
            onProgress(DownloadProgress(written, model.sizeBytes))

            val channel = response.bodyAsChannel()
            val buffer = ByteArray(BUFFER_BYTES)
            var reported = written
            SystemFileSystem.sink(partial, append = append).buffered().use { sink ->
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read < 0) break
                    sink.write(buffer, 0, read)
                    written += read
                    if (written - reported >= PROGRESS_STEP_BYTES) {
                        reported = written
                        onProgress(DownloadProgress(written, model.sizeBytes))
                    }
                }
            }
            onProgress(DownloadProgress(written, model.sizeBytes))
        }

        val size = sizeOf(partial) ?: 0L
        if (size != model.sizeBytes) {
            // Short: the connection dropped and the next call resumes. Long:
            // the server sent something else entirely, so start over.
            if (size > model.sizeBytes) SystemFileSystem.delete(partial, mustExist = false)
            throw LlmRuntimeException("download ended at $size of ${model.sizeBytes} bytes")
        }
        SystemFileSystem.atomicMove(partial, modelFile(model))
    }

    override suspend fun delete(model: ChatModel) = withContext(ioDispatcher) {
        SystemFileSystem.delete(modelFile(model), mustExist = false)
        SystemFileSystem.delete(partialFile(model), mustExist = false)
    }

    override suspend fun load(model: ChatModel) = withContext(nativeDispatcher) {
        if (loadedModelId == model.id) return@withContext
        native("loading ${model.fileName}") {
            if (loadedModelId != null) {
                LlamaBridge.shutdown()
                loadedModelId = null
            }
            // The load-time parameters (context, threads, mmap, batch) only
            // take effect if set before the model is initialized.
            applyParams(model.contextLength, GenerationOptions())
            if (!LlamaBridge.initGenerateModel(modelFile(model).toString())) {
                throw LlmRuntimeException("llama.cpp could not load ${model.fileName}")
            }
            loadedModelId = model.id
            contextLength = model.contextLength
        }
    }

    override suspend fun unload() = withContext(nativeDispatcher) {
        if (loadedModelId == null) return@withContext
        generationLock.withLock {
            native("unloading the model") { LlamaBridge.shutdown() }
            loadedModelId = null
        }
    }

    override fun complete(prompt: String, options: GenerationOptions): Flow<String> = callbackFlow {
        val worker = launch(nativeDispatcher) {
            generationLock.withLock {
                try {
                    if (loadedModelId == null) throw LlmRuntimeException("the model is not loaded")
                    applyParams(contextLength, options)
                    LlamaBridge.generateStream(prompt, object : GenStream {
                        override fun onDelta(text: String) {
                            trySend(text)
                        }

                        override fun onComplete() {
                            channel.close()
                        }

                        override fun onError(message: String) {
                            channel.close(LlmRuntimeException(message))
                        }
                    })
                    // generateStream blocks until the answer is finished, so by
                    // now a callback has normally closed the channel already.
                    channel.close()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    channel.close(LlmRuntimeException(error.message ?: "generation failed", error))
                }
            }
        }
        awaitClose {
            // The collector went away (the user pressed stop): ask llama.cpp to
            // stop, which makes the blocked generateStream call return.
            if (worker.isActive) LlamaBridge.nativeCancelGenerate()
        }
    }.buffer(capacity = Channel.UNLIMITED)

    private fun applyParams(contextLength: Int, options: GenerationOptions) {
        LlamaBridge.updateGenerateParams(
            temperature = options.temperature,
            maxTokens = options.maxTokens,
            topP = options.topP,
            topK = options.topK,
            repeatPenalty = options.repeatPenalty,
            contextLength = contextLength,
            numThreads = (processorCount() / 2).coerceIn(2, 4),
            useMmap = true,
            flashAttention = false,
            batchSize = 512,
            gpuLayers = 0,
        )
    }

    /**
     * Runs a call into the native library, turning what it can throw — an
     * `UnsatisfiedLinkError` on a device whose ABI it does not ship, an
     * `ExceptionInInitializerError` from its loader — into an exception the
     * UI reports instead of a crash.
     */
    private inline fun <T> native(action: String, block: () -> T): T = try {
        block()
    } catch (error: LlmRuntimeException) {
        throw error
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Throwable) {
        throw LlmRuntimeException("llama.cpp failed $action: ${error.message ?: error::class.simpleName}", error)
    }

    private companion object {
        const val BUFFER_BYTES = 256 * 1024
        const val PROGRESS_STEP_BYTES = 1_000_000L

        /** Room left for everything else on the device once the model is in. */
        const val STORAGE_HEADROOM_BYTES = 200_000_000L
    }
}
