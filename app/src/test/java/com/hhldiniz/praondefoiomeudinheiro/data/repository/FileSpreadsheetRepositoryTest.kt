package com.hhldiniz.praondefoiomeudinheiro.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.hhldiniz.praondefoiomeudinheiro.R
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CsvEntry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream

class FileSpreadsheetRepositoryTest {

    private lateinit var repository: FileSpreadsheetRepository
    private lateinit var contentResolver: ContentResolver
    private lateinit var context: Context

    @Before
    fun setUp() {
        repository = FileSpreadsheetRepository()
        contentResolver = mock()
        context = mock()
        whenever(context.contentResolver).thenReturn(contentResolver)
    }

    @Test
    fun validateFile_validCsv_returnsValidFile() = runTest {
        val uri = uri("transactions.csv")
        whenever(contentResolver.openInputStream(uri)).thenReturn(
            csvStream("Data,Valor,Descrição,Categoria\n01/01/2026,10,Café,Alimentação")
        )

        val report = repository.validateFile(uri, context)

        assertEquals(1, report.validFiles.size)
        assertEquals("transactions.csv", report.validFiles.single().name)
        assertEquals(listOf("Data", "Valor", "Descrição", "Categoria"), report.validFiles.single().headerColumns)
        assertTrue(report.invalidFiles.isEmpty())
    }

    @Test
    fun validateFiles_mixedFiles_aggregatesValidAndInvalidResults() = runTest {
        val validUri = uri("transactions.csv")
        val invalidUri = uri("notes.txt")
        whenever(contentResolver.openInputStream(validUri)).thenReturn(
            csvStream("Data,Valor\n01/01/2026,10")
        )
        whenever(context.getString(R.string.error_unsupported_format)).thenReturn("Unsupported format")

        val report = repository.validateFiles(listOf(validUri, invalidUri), context)

        assertEquals(listOf("transactions.csv"), report.validFiles.map { it.name })
        assertEquals(listOf("notes.txt"), report.invalidFiles.map { it.name })
        assertEquals("Unsupported format", report.invalidFiles.single().reason)
    }

    @Test
    fun validateFiles_emptyList_returnsEmptyReport() = runTest {
        val report = repository.validateFiles(emptyList(), context)

        assertTrue(report.validFiles.isEmpty())
        assertTrue(report.invalidFiles.isEmpty())
    }

    @Test
    fun readValues_structuredCsv_extractsSpendingAndEarnings() = runTest {
        val uri = uri("budget.csv")
        val csv = listOf(
            csvRow("Budget", "", "", "", "", "", "", "", "", ""),
            csvRow("", "Data", "Valor", "Descrição", "Categoria", "", "Data", "Valor", "Descrição", "Categoria"),
            csvRow("", "01/01/2026", "10,50", "Café", "Alimentação", "", "02/01/2026", "1000", "Salário", "Renda"),
            csvRow("", "03/01/2026", "25", "Ônibus", "Transporte"),
            csvRow("", "", "", "", "", "", "", "", "", ""),
        ).joinToString("\n")
        whenever(contentResolver.openInputStream(uri)).thenReturn(csvStream(csv))

        val result = repository.readValues(uri, contentResolver).getOrThrow()

        assertEquals("budget.csv!A1:Z2", result.range)
        assertEquals(2, result.rows.size)
        assertEquals(
            listOf(
                CsvEntry("01/01/2026", "10,50", "Café", "Alimentação"),
                CsvEntry("03/01/2026", "25", "Ônibus", "Transporte"),
            ),
            result.spendingEntries,
        )
        assertEquals(
            listOf(CsvEntry("02/01/2026", "1000", "Salário", "Renda")),
            result.earningsEntries,
        )
    }

    @Test
    fun readValues_simpleCsv_skipsFirstRowAndReturnsOnlySpending() = runTest {
        val uri = uri("simple.csv")
        val csv = listOf(
            csvRow("ignored", "date", "amount", "description", "category"),
            csvRow("", "01/02/2026", "42", "Book", "Education"),
        ).joinToString("\n")
        whenever(contentResolver.openInputStream(uri)).thenReturn(csvStream(csv))

        val result = repository.readValues(uri, contentResolver).getOrThrow()

        assertEquals("simple.csv!A1:Z1", result.range)
        assertEquals(listOf(CsvEntry("01/02/2026", "42", "Book", "Education")), result.spendingEntries)
        assertTrue(result.earningsEntries.isEmpty())
    }

    @Test
    fun readValues_streamCannotBeOpened_returnsFailure() = runTest {
        val uri = uri("missing.csv")
        whenever(contentResolver.openInputStream(uri)).thenReturn(null)

        val result = repository.readValues(uri, contentResolver)

        assertTrue(result.isFailure)
        assertEquals("Cannot open file", result.exceptionOrNull()?.message)
    }

    private fun uri(fileName: String): Uri = mock<Uri>().also {
        whenever(it.lastPathSegment).thenReturn(fileName)
    }

    private fun csvStream(content: String) =
        ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))

    private fun csvRow(vararg cells: String) = cells.joinToString(",") { cell ->
        if (cell.contains(',')) "\"$cell\"" else cell
    }
}
