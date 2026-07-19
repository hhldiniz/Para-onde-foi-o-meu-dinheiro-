package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.landing

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.hhldiniz.praondefoiomeudinheiro.data.local.CsvUriHolder
import com.hhldiniz.praondefoiomeudinheiro.domain.model.FileValidationReport
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvalidSpreadsheetFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.ValidSpreadsheetFile
import com.hhldiniz.praondefoiomeudinheiro.domain.repository.SpreadsheetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
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
 * A fake [SpreadsheetRepository] avoids touching the filesystem or Context so
 * the test run purely on the JVM.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LandingViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: SpreadsheetRepository
    private lateinit var context: Context
    private lateinit var viewModel: LandingViewModel

    private val fileUri: Uri = mock<Uri>().also {
        whenever(it.lastPathSegment).thenReturn("test.csv")
        whenever(it.toString()).thenReturn("content://test.csv")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mock()
        context = mock()
        val contentResolver = mock<ContentResolver>()
        whenever(context.contentResolver).thenReturn(contentResolver)
        viewModel = LandingViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        CsvUriHolder.uris = emptyList()
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun initialState_isIdle() = runTest {
        assertEquals(LandingUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun validUris_areEmptyInitially() {
        assertTrue(viewModel.validUris.isEmpty())
    }

    // -------------------------------------------------------------------------
    // onFilePicked – success path
    // -------------------------------------------------------------------------

    @Test
    fun onFilePicked_validFile_transitionsToValidationResult() = runTest {
        val report = FileValidationReport(
            validFiles = listOf(ValidSpreadsheetFile("test.csv", fileUri, listOf("Data", "Valor"))),
            invalidFiles = emptyList()
        )
        whenever(repository.validateFile(any(), any())).thenReturn(report)
        // Suppress SecurityException from takePersistableUriPermission
        whenever(context.contentResolver.takePersistableUriPermission(any(), any()))
            .then { }

        viewModel.onFilePicked(fileUri, context)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LandingUiState.ValidationResult)
        val result = state as LandingUiState.ValidationResult
        assertEquals(1, result.report.validFiles.size)
    }

    @Test
    fun onFilePicked_validFile_storesValidUris() = runTest {
        val report = FileValidationReport(
            validFiles = listOf(ValidSpreadsheetFile("test.csv", fileUri, emptyList())),
            invalidFiles = emptyList()
        )
        whenever(repository.validateFile(any(), any())).thenReturn(report)

        viewModel.onFilePicked(fileUri, context)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(fileUri), viewModel.validUris)
    }

    // -------------------------------------------------------------------------
    // onFilePicked – error path
    // -------------------------------------------------------------------------

    @Test
    fun onFilePicked_invalidFile_transitionsToError() = runTest {
        val report = FileValidationReport(
            validFiles = emptyList(),
            invalidFiles = listOf(
                InvalidSpreadsheetFile("bad.txt", fileUri, "Formato não suportado")
            )
        )
        whenever(repository.validateFile(any(), any())).thenReturn(report)

        viewModel.onFilePicked(fileUri, context)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LandingUiState.Error)
        assertEquals("Formato não suportado", (state as LandingUiState.Error).message)
    }

    @Test
    fun onFilePicked_invalidFile_withNoReason_usesDefaultMessage() = runTest {
        val report = FileValidationReport(
            validFiles = emptyList(),
            invalidFiles = emptyList()
        )
        whenever(repository.validateFile(any(), any())).thenReturn(report)
        whenever(context.getString(any())).thenReturn("No valid files")

        viewModel.onFilePicked(fileUri, context)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LandingUiState.Error)
    }

    @Test
    fun onFilePicked_setsLoadingIntermediately() = runTest {
        val report = FileValidationReport(
            validFiles = listOf(ValidSpreadsheetFile("test.csv", fileUri, emptyList())),
            invalidFiles = emptyList()
        )
        whenever(repository.validateFile(any(), any())).thenReturn(report)

        viewModel.onFilePicked(fileUri, context)
        // Before advancing scheduler, loading state should be set
        assertEquals(LandingUiState.Loading, viewModel.uiState.value)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    // -------------------------------------------------------------------------
    // onContinue
    // -------------------------------------------------------------------------

    @Test
    fun onContinue_storesUrisInCsvUriHolder() = runTest {
        val report = FileValidationReport(
            validFiles = listOf(ValidSpreadsheetFile("test.csv", fileUri, emptyList())),
            invalidFiles = emptyList()
        )
        whenever(repository.validateFile(any(), any())).thenReturn(report)
        viewModel.onFilePicked(fileUri, context)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onContinue()

        assertEquals(listOf(fileUri), CsvUriHolder.uris)
        assertEquals(LandingUiState.ProceedToHome, viewModel.uiState.value)
    }

    // -------------------------------------------------------------------------
    // onSkip
    // -------------------------------------------------------------------------

    @Test
    fun onSkip_clearsCsvUriHolderAndProceeds() {
        CsvUriHolder.uris = listOf(fileUri)

        viewModel.onSkip()

        assertTrue(CsvUriHolder.uris.isEmpty())
        assertEquals(LandingUiState.ProceedToHome, viewModel.uiState.value)
    }

    // -------------------------------------------------------------------------
    // onReset
    // -------------------------------------------------------------------------

    @Test
    fun onReset_clearsStateAndValidUris() = runTest {
        val report = FileValidationReport(
            validFiles = listOf(ValidSpreadsheetFile("test.csv", fileUri, emptyList())),
            invalidFiles = emptyList()
        )
        whenever(repository.validateFile(any(), any())).thenReturn(report)
        viewModel.onFilePicked(fileUri, context)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onReset()

        assertEquals(LandingUiState.Idle, viewModel.uiState.value)
        assertTrue(viewModel.validUris.isEmpty())
    }

    // -------------------------------------------------------------------------
    // onFolderPicked
    // -------------------------------------------------------------------------

    @Test
    fun onFolderPicked_emptyFolder_transitionsToError() = runTest {
        // When listCsvUris returns an empty list (DocumentFile is null for a mock URI)
        // the ViewModel calls context.getString for the error message
        whenever(context.getString(any())).thenReturn("Nenhum CSV na pasta")

        val treeUri: Uri = mock<Uri>().also {
            whenever(it.lastPathSegment).thenReturn("folder")
            whenever(it.toString()).thenReturn("content://folder")
        }

        viewModel.onFolderPicked(treeUri, context)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LandingUiState.Error)
    }
}
