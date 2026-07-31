package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.landing

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.local.SelectedFilesHolder
import com.hhldiniz.praondefoiomeudinheiro.domain.file.InMemoryPlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFolder
import com.hhldiniz.praondefoiomeudinheiro.domain.model.UiText
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.error_no_csv_in_folder
import com.hhldiniz.praondefoiomeudinheiro.domain.model.FileValidationReport
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvalidSpreadsheetFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.ValidSpreadsheetFile
import com.hhldiniz.praondefoiomeudinheiro.domain.repository.SpreadsheetRepository
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [LandingViewModel].
 *
 * A mocked [SpreadsheetRepository] and in-memory [PlatformFile]s keep the
 * tests off the filesystem, so they run purely on the JVM.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LandingViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: SpreadsheetRepository
    private lateinit var viewModel: LandingViewModel

    private val pickedFile: PlatformFile = InMemoryPlatformFile("test.csv")

    private fun folderOf(vararg files: PlatformFile) = object : PlatformFolder {
        override val name = "folder"
        override val identifier = "content://folder"
        override suspend fun listSpreadsheetFiles(): List<PlatformFile> = files.toList()
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        viewModel = LandingViewModel(repository, testDispatcher)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
        SelectedFilesHolder.files = emptyList()
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun initialState_isIdle() = runTest {
        assertEquals(LandingUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun validFiles_areEmptyInitially() {
        assertTrue(viewModel.validFiles.isEmpty())
    }

    // -------------------------------------------------------------------------
    // onFilePicked – success path
    // -------------------------------------------------------------------------

    @Test
    fun onFilePicked_validFile_transitionsToValidationResult() = runTest {
        val report = FileValidationReport(
            validFiles = listOf(ValidSpreadsheetFile("test.csv", pickedFile, listOf("Data", "Valor"))),
            invalidFiles = emptyList()
        )
        whenever(repository.validateFile(any())).thenReturn(report)

        viewModel.onFilePicked(pickedFile)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LandingUiState.ValidationResult)
        val result = state as LandingUiState.ValidationResult
        assertEquals(1, result.report.validFiles.size)
    }

    @Test
    fun onFilePicked_validFile_storesValidFiles() = runTest {
        val report = FileValidationReport(
            validFiles = listOf(ValidSpreadsheetFile("test.csv", pickedFile, emptyList())),
            invalidFiles = emptyList()
        )
        whenever(repository.validateFile(any())).thenReturn(report)

        viewModel.onFilePicked(pickedFile)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(pickedFile), viewModel.validFiles)
    }

    // -------------------------------------------------------------------------
    // onFilePicked – error path
    // -------------------------------------------------------------------------

    @Test
    fun onFilePicked_invalidFile_transitionsToError() = runTest {
        val report = FileValidationReport(
            validFiles = emptyList(),
            invalidFiles = listOf(
                InvalidSpreadsheetFile("bad.txt", pickedFile, UiText.Raw("Formato não suportado"))
            )
        )
        whenever(repository.validateFile(any())).thenReturn(report)

        viewModel.onFilePicked(pickedFile)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LandingUiState.Error)
        assertEquals(UiText.Raw("Formato não suportado"), (state as LandingUiState.Error).message)
    }

    @Test
    fun onFilePicked_invalidFile_withNoReason_usesDefaultMessage() = runTest {
        val report = FileValidationReport(
            validFiles = emptyList(),
            invalidFiles = emptyList()
        )
        whenever(repository.validateFile(any())).thenReturn(report)

        viewModel.onFilePicked(pickedFile)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LandingUiState.Error)
    }

    @Test
    fun onFilePicked_setsLoadingIntermediately() = runTest {
        val report = FileValidationReport(
            validFiles = listOf(ValidSpreadsheetFile("test.csv", pickedFile, emptyList())),
            invalidFiles = emptyList()
        )
        whenever(repository.validateFile(any())).thenReturn(report)

        viewModel.onFilePicked(pickedFile)
        // The validation coroutine is dispatched onto the test scheduler, so the
        // Loading state is set only once the scheduler is advanced.
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LandingUiState.ValidationResult)
    }

    // -------------------------------------------------------------------------
    // onContinue
    // -------------------------------------------------------------------------

    @Test
    fun onContinue_storesFilesInSelectedFilesHolder() = runTest {
        val report = FileValidationReport(
            validFiles = listOf(ValidSpreadsheetFile("test.csv", pickedFile, emptyList())),
            invalidFiles = emptyList()
        )
        whenever(repository.validateFile(any())).thenReturn(report)
        viewModel.onFilePicked(pickedFile)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onContinue()

        assertEquals(listOf(pickedFile), SelectedFilesHolder.files)
        assertEquals(LandingUiState.ProceedToHome, viewModel.uiState.value)
    }

    // -------------------------------------------------------------------------
    // onSkip
    // -------------------------------------------------------------------------

    @Test
    fun onSkip_clearsSelectedFilesHolderAndProceeds() {
        SelectedFilesHolder.files = listOf(pickedFile)

        viewModel.onSkip()

        assertTrue(SelectedFilesHolder.files.isEmpty())
        assertEquals(LandingUiState.ProceedToHome, viewModel.uiState.value)
    }

    // -------------------------------------------------------------------------
    // onReset
    // -------------------------------------------------------------------------

    @Test
    fun onReset_clearsStateAndValidFiles() = runTest {
        val report = FileValidationReport(
            validFiles = listOf(ValidSpreadsheetFile("test.csv", pickedFile, emptyList())),
            invalidFiles = emptyList()
        )
        whenever(repository.validateFile(any())).thenReturn(report)
        viewModel.onFilePicked(pickedFile)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onReset()

        assertEquals(LandingUiState.Idle, viewModel.uiState.value)
        assertTrue(viewModel.validFiles.isEmpty())
    }

    // -------------------------------------------------------------------------
    // onFolderPicked
    // -------------------------------------------------------------------------

    @Test
    fun onFolderPicked_emptyFolder_transitionsToError() = runTest {
        viewModel.onFolderPicked(folderOf())
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LandingUiState.Error)
        assertEquals(
            UiText.Localized(Res.string.error_no_csv_in_folder),
            (state as LandingUiState.Error).message,
        )
    }

    @Test
    fun onFolderPicked_noValidFiles_transitionsToError() = runTest {
        whenever(repository.validateFiles(any()))
            .thenReturn(FileValidationReport(emptyList(), emptyList()))

        viewModel.onFolderPicked(folderOf(pickedFile))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LandingUiState.Error)
    }
}
