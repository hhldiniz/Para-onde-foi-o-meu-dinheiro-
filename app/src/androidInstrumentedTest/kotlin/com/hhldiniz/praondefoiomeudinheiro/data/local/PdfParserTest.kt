package com.hhldiniz.praondefoiomeudinheiro.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class PdfParserTest {

    @Before
    fun setUp() {
        PdfBoxInitializer.init(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    /** Builds a one-page PDF where each [lines] entry is written as its own text line. */
    private fun buildPdf(lines: List<String>): ByteArray {
        val document = PDDocument()
        val page = PDPage()
        document.addPage(page)

        PDPageContentStream(document, page).use { stream ->
            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA, 12f)
            stream.newLineAtOffset(50f, 700f)
            lines.forEachIndexed { index, line ->
                if (index > 0) stream.newLineAtOffset(0f, -20f)
                stream.showText(line)
            }
            stream.endText()
        }

        val output = ByteArrayOutputStream()
        document.save(output)
        document.close()
        return output.toByteArray()
    }

    @Test
    fun parse_splitsColumnsOnWideGaps() = runTest {
        val pdf = buildPdf(listOf("Data      Valor      Descricao      Categoria"))
        val result = PdfParser.parse(pdf)

        assertEquals(1, result.size)
        assertEquals(listOf("Data", "Valor", "Descricao", "Categoria"), result[0])
    }

    @Test
    fun parse_returnsOneRowPerLine() = runTest {
        val pdf = buildPdf(
            listOf(
                "Data      Valor      Descricao      Categoria",
                "01/01/2024      100      Mercado      Alimentacao",
            )
        )
        val result = PdfParser.parse(pdf)

        assertEquals(2, result.size)
        assertEquals(listOf("01/01/2024", "100", "Mercado", "Alimentacao"), result[1])
    }

    @Test
    fun parse_emptyDocument_returnsNoRows() = runTest {
        val document = PDDocument()
        document.addPage(PDPage())
        val output = ByteArrayOutputStream()
        document.save(output)
        document.close()

        val result = PdfParser.parse(output.toByteArray())

        assertTrue(result.isEmpty())
    }

    @Test
    fun parse_singleSpacesWithinColumn_keepWordsTogether() = runTest {
        val pdf = buildPdf(listOf("01/01/2024      Compra no Mercado      Alimentacao"))
        val result = PdfParser.parse(pdf)

        assertEquals(listOf("01/01/2024", "Compra no Mercado", "Alimentacao"), result[0])
    }

    @Test
    fun parse_structuredTwoTableHeader_matchesExpectedColumnPositions() = runTest {
        // Mirrors the app's real spreadsheet layout: a "Despesas" section label in
        // column 0, then Data/Valor/Descricao/Categoria for spending (index 1-4), a
        // "Renda" label in column 5, then the same four headers again for earnings
        // (index 6-9) — the two-table layout TransactionColumnMapper detects by
        // finding a second "Data" column further along the row.
        val header = "Despesas      Data      Valor      Descricao      Categoria" +
            "      Renda      Data      Valor      Descricao      Categoria"
        val pdf = buildPdf(listOf(header))
        val result = PdfParser.parse(pdf)

        assertEquals(1, result.size)
        val row = result[0]
        assertTrue("expected at least 10 columns, got ${row.size}: $row", row.size >= 10)
        assertEquals("Data", row[1])
        assertEquals("Valor", row[2])
        assertEquals("Descricao", row[3])
        assertEquals("Categoria", row[4])
        assertEquals("Data", row[6])
        assertEquals("Valor", row[7])
        assertEquals("Descricao", row[8])
        assertEquals("Categoria", row[9])
    }

    @Test
    fun parse_multiPageDocument_returnsRowsFromAllPages() = runTest {
        val document = PDDocument()
        val firstPage = PDPage()
        val secondPage = PDPage()
        document.addPage(firstPage)
        document.addPage(secondPage)

        writeLine(document, firstPage, "Data      Valor      Descricao      Categoria")
        writeLine(document, secondPage, "02/02/2024      50      Farmacia      Saude")

        val output = ByteArrayOutputStream()
        document.save(output)
        document.close()

        val result = PdfParser.parse(output.toByteArray())

        assertEquals(2, result.size)
        assertEquals(listOf("Data", "Valor", "Descricao", "Categoria"), result[0])
        assertEquals(listOf("02/02/2024", "50", "Farmacia", "Saude"), result[1])
    }

    private fun writeLine(document: PDDocument, page: PDPage, line: String) {
        PDPageContentStream(document, page).use { stream ->
            stream.beginText()
            stream.setFont(PDType1Font.HELVETICA, 12f)
            stream.newLineAtOffset(50f, 700f)
            stream.showText(line)
            stream.endText()
        }
    }
}
