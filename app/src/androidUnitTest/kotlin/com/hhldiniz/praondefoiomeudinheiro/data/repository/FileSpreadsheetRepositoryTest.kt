package com.hhldiniz.praondefoiomeudinheiro.data.repository

import com.hhldiniz.praondefoiomeudinheiro.domain.file.InMemoryPlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.file.PlatformFile
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CsvEntry
import com.hhldiniz.praondefoiomeudinheiro.domain.model.UiText
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.error_header_not_found
import com.hhldiniz.praondefoiomeudinheiro.resources.error_unsupported_format
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FileSpreadsheetRepositoryTest {

    private lateinit var repository: FileSpreadsheetRepository

    @Before
    fun setUp() {
        repository = FileSpreadsheetRepository()
    }

    @Test
    fun validateFile_validCsv_returnsValidFile() = runTest {
        val file = csvFile("transactions.csv", "Data,Valor,Descrição,Categoria\n01/01/2026,10,Café,Alimentação")

        val report = repository.validateFile(file)

        assertEquals(1, report.validFiles.size)
        assertEquals("transactions.csv", report.validFiles.single().name)
        assertEquals(listOf("Data", "Valor", "Descrição", "Categoria"), report.validFiles.single().headerColumns)
        assertTrue(report.invalidFiles.isEmpty())
    }

    @Test
    fun validateFiles_mixedFiles_aggregatesValidAndInvalidResults() = runTest {
        val validFile = csvFile("transactions.csv", "Data,Valor\n01/01/2026,10")
        val invalidFile = csvFile("notes.txt", "whatever")

        val report = repository.validateFiles(listOf(validFile, invalidFile))

        assertEquals(listOf("transactions.csv"), report.validFiles.map { it.name })
        assertEquals(listOf("notes.txt"), report.invalidFiles.map { it.name })
        assertEquals(
            UiText.Localized(Res.string.error_unsupported_format),
            report.invalidFiles.single().reason,
        )
    }

    @Test
    fun validateFiles_emptyList_returnsEmptyReport() = runTest {
        val report = repository.validateFiles(emptyList())

        assertTrue(report.validFiles.isEmpty())
        assertTrue(report.invalidFiles.isEmpty())
    }

    @Test
    fun readValues_structuredCsv_extractsSpendingAndEarnings() = runTest {
        val fileName = "budget.csv"
        val csv = listOf(
            csvRow("Budget", "", "", "", "", "", "", "", "", ""),
            csvRow("", "Data", "Valor", "Descrição", "Categoria", "", "Data", "Valor", "Descrição", "Categoria"),
            csvRow("", "01/01/2026", "10,50", "Café", "Alimentação", "", "02/01/2026", "1000", "Salário", "Renda"),
            csvRow("", "03/01/2026", "25", "Ônibus", "Transporte"),
            csvRow("", "", "", "", "", "", "", "", "", ""),
        ).joinToString("\n")
        val file = csvFile(fileName, csv)

        val result = repository.readValues(file).getOrThrow()

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
        val fileName = "simple.csv"
        val csv = listOf(
            csvRow("ignored", "date", "amount", "description", "category"),
            csvRow("", "01/02/2026", "42", "Book", "Education"),
        ).joinToString("\n")
        val file = csvFile(fileName, csv)

        val result = repository.readValues(file).getOrThrow()

        assertEquals("simple.csv!A1:Z1", result.range)
        assertEquals(listOf(CsvEntry("01/02/2026", "42", "Book", "Education")), result.spendingEntries)
        assertTrue(result.earningsEntries.isEmpty())
    }

    @Test
    fun readValues_columnsInNonCanonicalOrder_stillMapsByName() = runTest {
        // Category first, then date, then description, then amount — an order
        // none of the fixed-index code ever supported.
        val fileName = "reordered.csv"
        val csv = listOf(
            csvRow("Categoria", "Data", "Descrição", "Valor"),
            csvRow("Alimentação", "01/01/2026", "Café", "10,50"),
        ).joinToString("\n")
        val file = csvFile(fileName, csv)

        val result = repository.readValues(file).getOrThrow()

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
        val fileName = "english.csv"
        val csv = listOf(
            csvRow("Date", "Amount", "Description", "Category", "Date", "Amount", "Description", "Category"),
            csvRow("01/01/2026", "10", "Coffee", "Food", "02/01/2026", "1000", "Salary", "Income"),
        ).joinToString("\n")
        val file = csvFile(fileName, csv)

        val result = repository.readValues(file).getOrThrow()

        assertEquals(listOf(CsvEntry("01/01/2026", "10", "Coffee", "Food")), result.spendingEntries)
        assertEquals(listOf(CsvEntry("02/01/2026", "1000", "Salary", "Income")), result.earningsEntries)
    }

    @Test
    fun readValues_missingCategoryColumn_defaultsToBlank() = runTest {
        val fileName = "no-category.csv"
        val csv = listOf(
            csvRow("Fecha", "Valor", "Descripción"),
            csvRow("01/01/2026", "10", "Café"),
        ).joinToString("\n")
        val file = csvFile(fileName, csv)

        val result = repository.readValues(file).getOrThrow()

        assertEquals(listOf(CsvEntry("01/01/2026", "10", "Café", "")), result.spendingEntries)
    }

    @Test
    fun validateFile_headerNamesInDifferentLanguageAndOrder_isRecognized() = runTest {
        val file = csvFile("spanish.csv", "Categoría,Fecha,Descripción,Valor\nComida,01/01/2026,Café,10")

        val report = repository.validateFile(file)

        assertEquals(1, report.validFiles.size)
        assertEquals(
            listOf("Categoría", "Fecha", "Descripción", "Valor"),
            report.validFiles.single().headerColumns,
        )
    }

    @Test
    fun validateFile_noRecognizableDateOrAmountColumn_isInvalid() = runTest {
        val file = csvFile("garbage.csv", "Foo,Bar,Baz\n1,2,3")

        val report = repository.validateFile(file)

        assertTrue(report.validFiles.isEmpty())
        assertEquals(
            UiText.Localized(Res.string.error_header_not_found),
            report.invalidFiles.single().reason,
        )
    }

    @Test
    fun readValues_fileCannotBeRead_returnsFailure() = runTest {
        val unreadable = object : PlatformFile {
            override val name = "missing.csv"
            override val identifier = "missing.csv"
            override suspend fun readBytes(): ByteArray = error("Cannot open file")
        }

        val result = repository.readValues(unreadable)

        assertTrue(result.isFailure)
        assertEquals("Cannot open file", result.exceptionOrNull()?.message)
    }

    private fun csvFile(fileName: String, content: String = ""): PlatformFile =
        InMemoryPlatformFile(fileName, content.encodeToByteArray())

    private fun csvRow(vararg cells: String) = cells.joinToString(",") { cell ->
        if (cell.contains(',')) "\"$cell\"" else cell
    }
}
