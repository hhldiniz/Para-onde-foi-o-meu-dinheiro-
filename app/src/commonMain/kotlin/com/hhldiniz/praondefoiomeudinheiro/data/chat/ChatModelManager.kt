package com.hhldiniz.praondefoiomeudinheiro.data.chat

import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatModel
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatModels
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.DownloadProgress
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.GenerationOptions
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.LlmRuntime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Where the assistant's model is in its life on this device. */
sealed interface ModelStatus {
    /** Looking for the file on the device. */
    data object Checking : ModelStatus

    /** Not on the device yet; the user has to agree to the download first. */
    data object NotDownloaded : ModelStatus

    data class Downloading(val progress: DownloadProgress) : ModelStatus

    /** On the device but not in memory. */
    data object Downloaded : ModelStatus

    /** Being read into memory, which takes a few seconds. */
    data object Loading : ModelStatus

    /** Loaded and answering. */
    data object Ready : ModelStatus

    data class Failed(val failure: ModelFailure, val detail: String?) : ModelStatus
}

/** What went wrong, which decides both the message and what "try again" does. */
enum class ModelFailure {
    /** The transfer failed; trying again resumes the download where the runtime can. */
    DOWNLOAD,

    /** The device has no room for the file. */
    STORAGE,

    /** The file is there but the runtime could not load it (most often: not enough memory). */
    LOAD,
}

/** The device has less free space than the model needs. */
class NotEnoughStorageException(val requiredBytes: Long, val availableBytes: Long) :
    Exception("needs $requiredBytes bytes of free space, $availableBytes available")

/**
 * Owns the assistant's model for the whole process: whether it is on the
 * device, downloading it, loading it and handing out generations.
 *
 * It is a singleton with its own scope, not part of the chat screen's
 * ViewModel, on purpose: a 1GB download has to survive the user leaving the
 * screen to look something up, and the native load must not be torn down
 * halfway because the screen went away. The conversation itself — which *is*
 * per screen — lives in `ChatViewModel`.
 */
class ChatModelManager(
    private val runtime: LlmRuntime,
    val model: ChatModel = ChatModels.DEFAULT,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _status = MutableStateFlow<ModelStatus>(ModelStatus.Checking)
    val status: StateFlow<ModelStatus> = _status.asStateFlow()

    /** Serializes everything that touches the runtime's file or loaded model. */
    private val lifecycle = Mutex()
    private var downloadJob: Job? = null

    /**
     * Checks whether the model is already on the device. Only moves the
     * status out of a resting state — a download or a loaded model is left as
     * it is.
     */
    fun refresh(): Job = scope.launch {
        lifecycle.withLock {
            val current = _status.value
            if (current is ModelStatus.Checking || current is ModelStatus.NotDownloaded || current is ModelStatus.Failed) {
                _status.value = try {
                    if (runtime.isDownloaded(model)) ModelStatus.Downloaded else ModelStatus.NotDownloaded
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    ModelStatus.NotDownloaded
                }
            }
        }
    }

    /** Starts (or resumes) the download. Does nothing while one is already running. */
    fun startDownload() {
        if (downloadJob?.isActive == true) return
        if (_status.value.let { it is ModelStatus.Downloaded || it is ModelStatus.Loading || it is ModelStatus.Ready }) return

        _status.value = ModelStatus.Downloading(DownloadProgress(0L, model.sizeBytes))
        downloadJob = scope.launch {
            try {
                runtime.download(model) { progress -> _status.value = ModelStatus.Downloading(progress) }
                _status.value = ModelStatus.Downloaded
            } catch (cancelled: CancellationException) {
                // Whatever arrived is kept by runtimes that can resume, so the
                // next start continues instead of beginning again.
                _status.value = ModelStatus.NotDownloaded
                throw cancelled
            } catch (error: NotEnoughStorageException) {
                _status.value = ModelStatus.Failed(ModelFailure.STORAGE, error.message)
            } catch (error: Exception) {
                _status.value = ModelStatus.Failed(ModelFailure.DOWNLOAD, error.message)
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
    }

    /**
     * Loads the model into memory if it is on the device and not loaded yet.
     * Runs in this manager's scope, so the caller going away does not abort a
     * native load midway.
     */
    fun load(): Job = scope.launch {
        lifecycle.withLock {
            val current = _status.value
            val canLoad = current is ModelStatus.Downloaded ||
                (current is ModelStatus.Failed && current.failure == ModelFailure.LOAD)
            if (!canLoad) return@withLock

            _status.value = ModelStatus.Loading
            _status.value = try {
                runtime.load(model)
                ModelStatus.Ready
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                ModelStatus.Failed(ModelFailure.LOAD, error.message)
            }
        }
    }

    /** Frees the model's memory; the file stays, so the next [load] needs no network. */
    fun unload(): Job = scope.launch {
        lifecycle.withLock {
            if (_status.value !is ModelStatus.Ready) return@withLock
            runCatching { runtime.unload() }
            _status.value = ModelStatus.Downloaded
        }
    }

    /** Removes the model from the device, stopping a download in progress first. */
    fun delete(): Job = scope.launch {
        downloadJob?.let {
            it.cancel()
            it.join()
        }
        lifecycle.withLock {
            runCatching { runtime.unload() }
            runCatching { runtime.delete(model) }
            _status.value = ModelStatus.NotDownloaded
        }
    }

    /**
     * Streams the model's continuation of [prompt]. Fails fast when the model
     * is not loaded, rather than waiting for a load nobody started.
     */
    fun generate(prompt: String, options: GenerationOptions): Flow<String> = flow {
        check(_status.value is ModelStatus.Ready) { "the model is not loaded" }
        runtime.complete(prompt, options).collect { emit(it) }
    }
}
