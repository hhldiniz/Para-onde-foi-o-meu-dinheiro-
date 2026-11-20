package com.hhldiniz.praondefoiomeudinheiro.data.chat

import com.hhldiniz.praondefoiomeudinheiro.domain.chat.ChatModels
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.DownloadProgress
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.GenerationOptions
import com.hhldiniz.praondefoiomeudinheiro.domain.chat.LlmRuntimeException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatModelManagerTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val model = ChatModels.DEFAULT

    private fun manager(runtime: FakeLlmRuntime) = ChatModelManager(runtime, model, CoroutineScope(dispatcher))

    @Test
    fun `refresh finds out whether the model is on the device`() = runTest(dispatcher) {
        assertEquals(ModelStatus.NotDownloaded, manager(FakeLlmRuntime(downloaded = false)).also { it.refresh() }.status.value)
        assertEquals(ModelStatus.Downloaded, manager(FakeLlmRuntime(downloaded = true)).also { it.refresh() }.status.value)
    }

    @Test
    fun `download reports progress and ends downloaded`() = runTest(dispatcher) {
        val runtime = FakeLlmRuntime().apply { downloadGate = CompletableDeferred() }
        val manager = manager(runtime)
        manager.refresh()

        manager.startDownload()
        assertEquals(
            ModelStatus.Downloading(DownloadProgress(model.sizeBytes / 4, model.sizeBytes)),
            manager.status.value,
        )

        runtime.downloadGate!!.complete(Unit)
        assertEquals(ModelStatus.Downloaded, manager.status.value)
    }

    @Test
    fun `a cancelled download goes back to not downloaded`() = runTest(dispatcher) {
        val runtime = FakeLlmRuntime().apply { downloadGate = CompletableDeferred() }
        val manager = manager(runtime)

        manager.startDownload()
        manager.cancelDownload()

        assertEquals(ModelStatus.NotDownloaded, manager.status.value)
    }

    @Test
    fun `download failures say whether it was the network or the storage`() = runTest(dispatcher) {
        val network = manager(FakeLlmRuntime().apply { downloadError = LlmRuntimeException("HTTP 503") })
        network.startDownload()
        assertEquals(ModelStatus.Failed(ModelFailure.DOWNLOAD, "HTTP 503"), network.status.value)

        val storage = manager(FakeLlmRuntime().apply { downloadError = NotEnoughStorageException(10, 1) })
        storage.startDownload()
        assertEquals(ModelFailure.STORAGE, (storage.status.value as ModelStatus.Failed).failure)
    }

    @Test
    fun `load makes the model ready, once`() = runTest(dispatcher) {
        val runtime = FakeLlmRuntime(downloaded = true)
        val manager = manager(runtime)
        manager.refresh()

        manager.load()
        manager.load()

        assertEquals(ModelStatus.Ready, manager.status.value)
        assertEquals(1, runtime.loads)
    }

    @Test
    fun `a failed load can be retried`() = runTest(dispatcher) {
        val runtime = FakeLlmRuntime(downloaded = true).apply { loadError = LlmRuntimeException("out of memory") }
        val manager = manager(runtime)
        manager.refresh()

        manager.load()
        assertEquals(ModelStatus.Failed(ModelFailure.LOAD, "out of memory"), manager.status.value)

        runtime.loadError = null
        manager.load()
        assertEquals(ModelStatus.Ready, manager.status.value)
    }

    @Test
    fun `unload keeps the file and delete removes it`() = runTest(dispatcher) {
        val runtime = FakeLlmRuntime(downloaded = true)
        val manager = manager(runtime)
        manager.refresh()
        manager.load()

        manager.unload()
        assertEquals(ModelStatus.Downloaded, manager.status.value)
        assertEquals(1, runtime.unloads)

        manager.delete()
        assertEquals(ModelStatus.NotDownloaded, manager.status.value)
        assertEquals(1, runtime.deletes)
    }

    @Test
    fun `generate only runs on a loaded model`() = runTest(dispatcher) {
        val runtime = FakeLlmRuntime(downloaded = true).apply { answer = listOf("a", "b") }
        val manager = manager(runtime)
        manager.refresh()

        val beforeLoad = runCatching { manager.generate("p", GenerationOptions()).toList() }
        assertTrue(beforeLoad.isFailure)

        manager.load()
        assertEquals(listOf("a", "b"), manager.generate("p", GenerationOptions()).toList())
    }
}
