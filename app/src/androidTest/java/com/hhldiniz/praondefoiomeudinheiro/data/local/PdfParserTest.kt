package com.hhldiniz.praondefoiomeudinheiro.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class PdfParserTest {

    @Before
    fun setUp() {
        PdfParser.init(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    /** Builds a one-page PDF where each [lines] entry is written as its own text line. */
    private fun buildPdf(lines: List<String>): ByteArrayInputStream {
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
        return ByteArrayInputStream(output.toByteArray())
    }

    @Test
    fun parse_splitsColumnsOnWideGaps() {
        val pdf = buildPdf(listOf("Data      Valor      Descricao      Categoria"))
        val result = PdfParser.parse(pdf)

        assertEquals(1, result.size)
        assertEquals(listOf("Data", "Valor", "Descricao", "Categoria"), result[0])
    }

    @Test
    fun parse_returnsOneRowPerLine() {
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
    fun parse_emptyDocument_returnsNoRows() {
        val document = PDDocument()
        document.addPage(PDPage())
        val output = ByteArrayOutputStream()
        document.save(output)
        document.close()

        val result = PdfParser.parse(ByteArrayInputStream(output.toByteArray()))

        assertTrue(result.isEmpty())
    }
}
