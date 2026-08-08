package com.hhldiniz.praondefoiomeudinheiro.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the reconstruction of whitespace-padded page text from a PDF
 * engine's positioned runs — the wasmJs half of the PDF import, which PDFBox
 * and PDFKit hand over already done.
 *
 * The contract every case here checks is the one
 * [PdfParser.splitIntoRows] depends on: a column boundary must come out as
 * two or more spaces, and everything inside a cell must not.
 */
class PdfTextLayoutTest {

    /** Runs for one row of a table, laid out like a text-drawing PDF would. */
    private fun row(baseline: Float, vararg cells: Pair<Float, String>, size: Float = 11f): List<PdfTextRun> =
        cells.map { (x, text) -> PdfTextRun(text, x = x, y = baseline, width = text.length * size * 0.5f) }

    private fun columnsOf(text: String): List<List<String>> = PdfParser.splitIntoRows(text)

    @Test
    fun `pads wide gaps into column separators and leaves word spacing alone`() {
        val runs = row(700f, 60f to "01/03/2026", 180f to "Padaria Sao Jorge", 420f to "-18,90")

        val columns = columnsOf(PdfTextLayout.reconstruct(runs))

        assertEquals(listOf(listOf("01/03/2026", "Padaria Sao Jorge", "-18,90")), columns)
    }

    /**
     * The regression this class exists for. pdf.js does not leave the space
     * between two columns implicit: it reports it as a run of its own whose
     * text is a single space and whose width spans the whole gap. Measuring
     * gaps with those runs in place makes every gap come out as zero, so no
     * padding is inserted and the row parses as one single column — which is
     * exactly how a PDF came to import as nothing at all.
     */
    @Test
    fun `ignores the blank runs a PDF engine reports for the gaps themselves`() {
        val runs = listOf(
            PdfTextRun("Data", x = 60f, y = 700f, width = 23.232f),
            PdfTextRun(" ", x = 83.232f, y = 700f, width = 96.768f),
            PdfTextRun("Descricao", x = 180f, y = 700f, width = 48.895f),
            PdfTextRun(" ", x = 228.895f, y = 700f, width = 191.105f),
            PdfTextRun("Valor", x = 420f, y = 700f, width = 25.674f),
        )

        val text = PdfTextLayout.reconstruct(runs)

        assertEquals(listOf(listOf("Data", "Descricao", "Valor")), columnsOf(text))
    }

    @Test
    fun `splits the same layout the same way regardless of font size`() {
        // The same table drawn at 7pt and at 21pt: every coordinate scales,
        // so the column boundaries must land in the same places.
        fun table(size: Float) = row(
            700f,
            (60f * size / 7f) to "05/02/2026",
            (130f * size / 7f) to "TRANSFERENCIA PIX",
            (300f * size / 7f) to "1.250,00",
            size = size,
        )

        val small = columnsOf(PdfTextLayout.reconstruct(table(7f)))
        val large = columnsOf(PdfTextLayout.reconstruct(table(21f)))

        assertEquals(listOf(listOf("05/02/2026", "TRANSFERENCIA PIX", "1.250,00")), small)
        assertEquals(small, large)
    }

    @Test
    fun `keeps a cell split across several runs in one column`() {
        // Kerning and font switches make an engine emit one cell as several
        // runs sitting flush against each other; they must not become columns.
        val runs = listOf(
            PdfTextRun("MERCADO", x = 130f, y = 680f, width = 44f),
            PdfTextRun(" ", x = 174f, y = 680f, width = 2.75f),
            PdfTextRun("LIVRE", x = 176.75f, y = 680f, width = 27.5f),
            PdfTextRun("-349,99", x = 420f, y = 680f, width = 38.5f),
        )

        assertEquals(
            listOf(listOf("MERCADO LIVRE", "-349,99")),
            columnsOf(PdfTextLayout.reconstruct(runs)),
        )
    }

    @Test
    fun `orders lines top to bottom, against the PDF's upward axis`() {
        // Rows handed over bottom-first, as a content stream may well emit them.
        val runs = row(660f, 60f to "linha3") + row(700f, 60f to "linha1") + row(680f, 60f to "linha2")

        assertEquals("linha1\nlinha2\nlinha3", PdfTextLayout.reconstruct(runs))
    }

    @Test
    fun `groups runs whose baselines differ only by rounding into one line`() {
        val runs = listOf(
            PdfTextRun("07/02/2026", x = 60f, y = 700f, width = 55f),
            PdfTextRun("NETFLIX.COM", x = 180f, y = 701.4f, width = 60f),
        )

        assertEquals(listOf(listOf("07/02/2026", "NETFLIX.COM")), columnsOf(PdfTextLayout.reconstruct(runs)))
    }

    @Test
    fun `measures each line against its own text size`() {
        // The very same 16pt gap, on two lines of one page: word spacing under
        // a 24pt heading, a column boundary in an 8pt table. A page-wide
        // threshold has to get one of the two wrong.
        val heading = listOf(
            PdfTextRun("Extrato", x = 60f, y = 760f, width = 84f),
            PdfTextRun("Fevereiro", x = 160f, y = 760f, width = 108f),
        )
        val entry = listOf(
            PdfTextRun("18/02/2026", x = 60f, y = 700f, width = 40f),
            PdfTextRun("POSTO IPIRANGA", x = 116f, y = 700f, width = 56f),
        )

        val rows = columnsOf(PdfTextLayout.reconstruct(heading + entry))

        assertEquals(listOf("Extrato Fevereiro"), rows[0])
        assertEquals(listOf("18/02/2026", "POSTO IPIRANGA"), rows[1])
    }

    @Test
    fun `handles pages with nothing to lay out`() {
        assertEquals("", PdfTextLayout.reconstruct(emptyList()))
        assertEquals("", PdfTextLayout.reconstruct(listOf(PdfTextRun("   ", x = 0f, y = 0f, width = 30f))))
    }

    @Test
    fun `survives runs an engine reports with no width`() {
        // Nothing to measure a character from; the layout must still produce
        // the row rather than dividing by zero.
        val runs = listOf(
            PdfTextRun("21/02/2026", x = 60f, y = 700f, width = 0f),
            PdfTextRun("FARMACIA", x = 180f, y = 700f, width = 0f),
        )

        val text = PdfTextLayout.reconstruct(runs)

        assertTrue(text.isNotEmpty())
        assertEquals(listOf(listOf("21/02/2026", "FARMACIA")), columnsOf(text))
    }
}
