package com.hhldiniz.praondefoiomeudinheiro.data.vision

import com.hhldiniz.praondefoiomeudinheiro.domain.vision.BoundingBox
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedDocument
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises the geometric half of the automatic import with hand-placed words,
 * standing in for what a text recognizer returns for a photographed statement.
 */
class DocumentLayoutAnalyzerTest {

    private val lineHeight = 0.03f

    private fun word(text: String, left: Float, top: Float, right: Float) =
        RecognizedWord(text, BoundingBox(left, top, right, top + lineHeight))

    /** A three-column statement: date, description (several words), amount. */
    private fun statementWords(): List<RecognizedWord> = listOf(
        word("Data", 0.05f, 0.10f, 0.13f),
        word("Descrição", 0.30f, 0.10f, 0.44f),
        word("Valor", 0.75f, 0.10f, 0.84f),

        word("15/03/2024", 0.05f, 0.20f, 0.20f),
        word("Mercado", 0.30f, 0.20f, 0.40f),
        word("Bom", 0.405f, 0.20f, 0.44f),
        word("Preço", 0.445f, 0.20f, 0.49f),
        word("R$", 0.75f, 0.20f, 0.79f),
        word("120,50", 0.795f, 0.20f, 0.88f),

        word("16/03/2024", 0.05f, 0.30f, 0.20f),
        word("Uber", 0.30f, 0.30f, 0.36f),
        word("R$", 0.79f, 0.30f, 0.83f),
        word("32,00", 0.835f, 0.30f, 0.88f),
    )

    @Test
    fun analyze_rebuildsRowsAndColumns() {
        val grid = DocumentLayoutAnalyzer.analyze(RecognizedDocument(statementWords()))

        assertEquals(3, grid.columns.size)
        assertEquals(3, grid.rows.size)
        assertEquals(listOf("Data", "Descrição", "Valor"), grid.rows[0])
        assertEquals(listOf("15/03/2024", "Mercado Bom Preço", "R$ 120,50"), grid.rows[1])
        assertEquals(listOf("16/03/2024", "Uber", "R$ 32,00"), grid.rows[2])
    }

    @Test
    fun analyze_wordsOfOneCellAreJoined_wordsOfDifferentColumnsAreNot() {
        val grid = DocumentLayoutAnalyzer.analyze(RecognizedDocument(statementWords()))

        // The three description words sit close together and become one cell…
        assertEquals("Mercado Bom Preço", grid.rows[1][1])
        // …while the wide gap before the amount keeps it in its own column.
        assertEquals("R$ 120,50", grid.rows[1][2])
    }

    @Test
    fun analyze_rightAlignedAmountsOfDifferentWidthsShareAColumn() {
        // Their centres are far apart (0.80 vs 0.86); only the whitespace
        // corridor on their left tells the layout analyzer they are one column.
        val words = statementWords() + listOf(
            word("17/03/2024", 0.05f, 0.40f, 0.20f),
            word("Café", 0.30f, 0.40f, 0.36f),
            word("R$", 0.82f, 0.40f, 0.845f),
            word("9,90", 0.85f, 0.40f, 0.88f),
        )

        val grid = DocumentLayoutAnalyzer.analyze(RecognizedDocument(words))

        assertEquals(3, grid.columns.size)
        assertEquals("R$ 9,90", grid.rows[3][2])
    }

    @Test
    fun analyze_pageWideTitleDoesNotCollapseTheColumns() {
        val words = listOf(
            word("Extrato bancário de março de 2024", 0.05f, 0.02f, 0.92f),
        ) + statementWords()

        val grid = DocumentLayoutAnalyzer.analyze(RecognizedDocument(words))

        assertEquals(3, grid.columns.size)
        assertEquals(4, grid.rows.size)
        assertTrue(grid.rows[0].any { it.contains("Extrato") })
    }

    @Test
    fun analyze_slightlySlopedLinesStayTogether() {
        // A photographed page is never perfectly straight; a drift well under
        // one line height must not split a row in two.
        val words = listOf(
            word("15/03/2024", 0.05f, 0.200f, 0.20f),
            word("Uber", 0.30f, 0.204f, 0.36f),
            word("R$ 32,00", 0.75f, 0.208f, 0.88f),
        )

        val grid = DocumentLayoutAnalyzer.analyze(RecognizedDocument(words))

        assertEquals(1, grid.rows.size)
        assertEquals(3, grid.rows[0].size)
    }

    @Test
    fun analyze_emptyDocument_producesEmptyGrid() {
        val grid = DocumentLayoutAnalyzer.analyze(RecognizedDocument(emptyList()))

        assertTrue(grid.isEmpty)
        assertTrue(grid.columns.isEmpty())
    }
}
