package com.hhldiniz.praondefoiomeudinheiro.data.chat

import com.hhldiniz.praondefoiomeudinheiro.data.local.prefs.InMemoryKeyValueStore
import com.hhldiniz.praondefoiomeudinheiro.data.local.prefs.KeyValueStore
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatModel
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatModels
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.DownloadProgress
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.GenerationOptions
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.LlmRuntime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

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

    /**
     * The last load closed the app (the system killed it, almost always for
     * memory). Unlike [LOAD] it is not retried on its own when the chat is
     * opened again — that would only crash again — but waits for the user.
     */
    CRASHED,
}

/** A failure of loading the model, as opposed to getting it onto the device: retrying means loading again. */
val ModelFailure.isLoadFailure: Boolean
    get() = this == ModelFailure.LOAD || this == ModelFailure.CRASHED

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
    /**
     * How long a load may take before it is reported as failed. Loading
     * normally takes seconds; this only exists so that a runtime that never
     * answers — a browser worker killed for using too much memory, a native
     * call that does not return — ends in an error the user can retry from,
     * instead of an endless "loading" screen.
     */
    private val loadTimeout: Duration = DEFAULT_LOAD_TIMEOUT,
    /**
     * Where a load in progress is written down before it starts and erased
     * once it ends, either way. Running out of memory while loading does not
     * fail the load — the system kills the app (on an iPhone, Safari's page),
     * and a load cannot report a crash that ended it. Finding the mark still
     * there on the next [refresh] is how that crash is told apart from a
     * model that was simply never loaded; without it, opening the chat again
     * would load again by itself and crash again, in a loop.
     */
    private val loadMarker: KeyValueStore = InMemoryKeyValueStore(),
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
            val recheck = current is ModelStatus.Checking || current is ModelStatus.NotDownloaded ||
                (current is ModelStatus.Failed && current.failure != ModelFailure.CRASHED)
            if (recheck) {
                val downloaded = try {
                    runtime.isDownloaded(model)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    false
                }
                _status.value = when {
                    !downloaded -> ModelStatus.NotDownloaded
                    // The last load never got to erase its mark: it took the
                    // app down with it. Wait for the user rather than repeat it.
                    loadMarker.getString(LOADING_KEY) == model.id -> ModelStatus.Failed(ModelFailure.CRASHED, null)
                    else -> ModelStatus.Downloaded
                }
                loadMarker.putString(LOADING_KEY, "")
            }
            if (current is ModelStatus.Checking) deleteOtherModels()
        }
    }

    /**
     * Removes any other model a previous version of the app downloaded (see
     * [ChatModels.ALL]) — a model switch must not leave the old file behind.
     * Best effort, since a leftover only costs space.
     */
    private suspend fun deleteOtherModels() {
        for (other in ChatModels.ALL) {
            if (other.id == model.id) continue
            try {
                if (runtime.isDownloaded(other)) runtime.delete(other)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
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
                (current is ModelStatus.Failed && current.failure.isLoadFailure)
            if (!canLoad) return@withLock

            _status.value = ModelStatus.Loading
            _status.value = loadWithin(loadTimeout)
        }
    }

    /**
     * Runs [LlmRuntime.load] and waits for it at most [timeout].
     *
     * The load runs as its own job and is only *awaited* under the timeout,
     * because a blocking native load cannot be interrupted: wrapping the call
     * itself in a timeout would wait for it to return anyway. A load that
     * outlives the timeout is cancelled as far as the runtime allows; a retry
     * then either picks up the model it finished loading in the meantime or
     * starts over.
     */
    private suspend fun loadWithin(timeout: Duration): ModelStatus {
        loadMarker.putString(LOADING_KEY, model.id)
        try {
            return awaitLoad(timeout)
        } finally {
            loadMarker.putString(LOADING_KEY, "")
        }
    }

    private suspend fun awaitLoad(timeout: Duration): ModelStatus {
        val result = CompletableDeferred<Unit>()
        val loading = scope.launch {
            try {
                runtime.load(model)
                result.complete(Unit)
            } catch (error: Throwable) {
                result.completeExceptionally(error)
            }
        }
        return try {
            if (withTimeoutOrNull(timeout) { result.await() } != null) {
                ModelStatus.Ready
            } else {
                loading.cancel()
                ModelStatus.Failed(ModelFailure.LOAD, "loading did not finish within $timeout")
            }
        } catch (error: Exception) {
            if (error is CancellationException && !result.isCompleted) throw error
            ModelStatus.Failed(ModelFailure.LOAD, error.message)
        }
    }

    /**
     * Frees the model's memory; the file stays, so the next [load] needs no
     * network.
     *
     * A failed load goes back to [ModelStatus.Downloaded] as well, so the
     * next visit to the chat tries again on its own instead of showing an old
     * failure. The runtime is not asked to unload in that case: after a
     * timeout it may still be stuck in the load, and waiting for it here
     * would hold [lifecycle] — and with it every later [load] — for as long
     * as it stays stuck. Each runtime's `load` discards a half-loaded engine
     * itself.
     */
    fun unload(): Job = scope.launch {
        lifecycle.withLock {
            when (val current = _status.value) {
                is ModelStatus.Ready -> {
                    runCatching { runtime.unload() }
                    _status.value = ModelStatus.Downloaded
                }
                is ModelStatus.Failed -> if (current.failure == ModelFailure.LOAD) _status.value = ModelStatus.Downloaded
                else -> Unit
            }
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

    companion object {
        /** Several times what a slow phone or browser needs to read the file into memory. */
        val DEFAULT_LOAD_TIMEOUT: Duration = 3.minutes

        /** The [KeyValueStore] the load mark lives in. */
        const val PREFS_NAME = "chat_model"

        private const val LOADING_KEY = "loading_model"
    }
}
