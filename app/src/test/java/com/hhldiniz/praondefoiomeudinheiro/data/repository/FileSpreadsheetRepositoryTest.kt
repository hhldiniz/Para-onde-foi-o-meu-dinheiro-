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
    fun readValues_simpleCsvWithNamedHeaders_mapsColumnsByName() = runTest {
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
    fun readValues_columnsInNonCanonicalOrder_stillMapsByName() = runTest {
        // Category first, then date, then description, then amount — an order
        // none of the fixed-index code ever supported.
        val uri = uri("reordered.csv")
        val csv = listOf(
            csvRow("Categoria", "Data", "Descrição", "Valor"),
            csvRow("Alimentação", "01/01/2026", "Café", "10,50"),
        ).joinToString("\n")
        whenever(contentResolver.openInputStream(uri)).thenReturn(csvStream(csv))

        val result = repository.readValues(uri, contentResolver).getOrThrow()

        assertEquals(
            listOf(CsvEntry("01/01/2026", "10,50", "Café", "Alimentação")),
            result.spendingEntries,
        )
    }

    @Test
    fun readValues_englishHeadersRepeatedTwice_splitsIntoSpendingAndEarningsByPosition() = runTest {
        // Two side-by-side tables in English, with no "Despesas"/"Renda"
        // marker words anywhere — the old implementation required those
        // literal Portuguese markers to even attempt the two-table layout.
        val uri = uri("english.csv")
        val csv = listOf(
            csvRow("Date", "Amount", "Description", "Category", "Date", "Amount", "Description", "Category"),
            csvRow("01/01/2026", "10", "Coffee", "Food", "02/01/2026", "1000", "Salary", "Income"),
        ).joinToString("\n")
        whenever(contentResolver.openInputStream(uri)).thenReturn(csvStream(csv))

        val result = repository.readValues(uri, contentResolver).getOrThrow()

        assertEquals(listOf(CsvEntry("01/01/2026", "10", "Coffee", "Food")), result.spendingEntries)
        assertEquals(listOf(CsvEntry("02/01/2026", "1000", "Salary", "Income")), result.earningsEntries)
    }

    @Test
    fun readValues_missingCategoryColumn_defaultsToBlank() = runTest {
        val uri = uri("no-category.csv")
        val csv = listOf(
            csvRow("Fecha", "Valor", "Descripción"),
            csvRow("01/01/2026", "10", "Café"),
        ).joinToString("\n")
        whenever(contentResolver.openInputStream(uri)).thenReturn(csvStream(csv))

        val result = repository.readValues(uri, contentResolver).getOrThrow()

        assertEquals(listOf(CsvEntry("01/01/2026", "10", "Café", "")), result.spendingEntries)
    }

    @Test
    fun validateFile_headerNamesInDifferentLanguageAndOrder_isRecognized() = runTest {
        val uri = uri("spanish.csv")
        whenever(contentResolver.openInputStream(uri)).thenReturn(
            csvStream("Categoría,Fecha,Descripción,Valor\nComida,01/01/2026,Café,10")
        )

        val report = repository.validateFile(uri, context)

        assertEquals(1, report.validFiles.size)
        assertEquals(
            listOf("Categoría", "Fecha", "Descripción", "Valor"),
            report.validFiles.single().headerColumns,
        )
    }

    @Test
    fun validateFile_noRecognizableDateOrAmountColumn_isInvalid() = runTest {
        val uri = uri("garbage.csv")
        whenever(contentResolver.openInputStream(uri)).thenReturn(
            csvStream("Foo,Bar,Baz\n1,2,3")
        )
        whenever(context.getString(R.string.error_header_not_found)).thenReturn("No header found")

        val report = repository.validateFile(uri, context)

        assertTrue(report.validFiles.isEmpty())
        assertEquals("No header found", report.invalidFiles.single().reason)
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
