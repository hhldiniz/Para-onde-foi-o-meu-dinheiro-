package com.hhldiniz.praondefoiomeudinheiro.data.vision

import com.hhldiniz.praondefoiomeudinheiro.domain.vision.BoundingBox
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedDocument
import com.hhldiniz.praondefoiomeudinheiro.domain.vision.RecognizedWord

/**
 * Reconstructs a table out of loose recognized words, using only where they
 * sit on the page.
 *
 * Text recognizers return words with boxes and nothing else: no rows, no
 * columns, no reading order that survives a multi-column layout. This is the
 * classic document-layout-analysis step that turns that word cloud back into
 * the `List<List<String>>` grid the rest of the importer already speaks — the
 * same shape [com.hhldiniz.praondefoiomeudinheiro.data.local.CsvParser],
 * `OdsParser` and `PdfParser` produce — so a photo of a statement and a `.csv`
 * of the same statement reach
 * [TransactionFieldClassifier] in identical form.
 *
 * Three passes, all of them scale-free because every threshold is expressed
 * as a multiple of the page's median word height (boxes arrive normalized to
 * `0f..1f`, see [BoundingBox]):
 *
 * 1. **Lines** — words are swept top-to-bottom and cut into lines wherever the
 *    vertical centre jumps by more than [LINE_TOLERANCE] word heights.
 * 2. **Cells** — inside a line, neighbouring words are glued together while
 *    the horizontal gap between them stays under [CELL_GAP] word heights; a
 *    wider gap means the layout meant them as separate cells.
 * 3. **Columns** — the horizontal extents of all cells are merged into bands,
 *    and the whitespace corridors between those bands (at least
 *    [COLUMN_CORRIDOR] word heights wide, and never crossed by any cell)
 *    become the column boundaries. This is a projection-profile analysis, and
 *    it copes with mixed alignment: left-aligned descriptions and
 *    right-aligned amounts still land in the right band even though their
 *    centres are nowhere near each other.
 */
object DocumentLayoutAnalyzer {

    /** Vertical slack, in median word heights, for two words to share a line. */
    private const val LINE_TOLERANCE = 0.6f

    /** Horizontal gap, in median word heights, that still counts as a space inside one cell. */
    private const val CELL_GAP = 0.9f

    /** Whitespace corridor, in median word heights, that separates two columns. */
    private const val COLUMN_CORRIDOR = 1.2f

    /**
     * Cells wider than this fraction of the page are ignored while columns are
     * being laid out: a headline or a footer sentence spans every corridor and
     * would otherwise collapse the whole table into a single column. They are
     * still placed into a column afterwards.
     */
    private const val WIDE_CELL_FRACTION = 0.55f

    /** Fallback word height for the degenerate case of zero-height boxes. */
    private const val FALLBACK_HEIGHT = 0.02f

    /** One cell of the reconstructed table: the text plus where it came from. */
    data class Cell(
        val text: String,
        val box: BoundingBox,
        val confidence: Float,
    )

    /** A horizontal band of the page holding one column of the table. */
    data class ColumnBand(val left: Float, val right: Float) {
        fun contains(x: Float) = x in left..right
        fun distanceTo(x: Float) = when {
            x < left -> left - x
            x > right -> x - right
            else -> 0f
        }
    }

    /** The reconstructed table plus the geometry it was derived from. */
    data class DocumentGrid(
        val rows: List<List<String>>,
        val columns: List<ColumnBand>,
        val averageConfidence: Float,
    ) {
        val isEmpty: Boolean get() = rows.isEmpty()
    }

    /** Runs all three passes over [document]. */
    fun analyze(document: RecognizedDocument): DocumentGrid {
        val words = document.words.filter { it.text.isNotBlank() }
        if (words.isEmpty()) return DocumentGrid(emptyList(), emptyList(), 0f)

        val medianHeight = medianHeight(words)
        val lines = groupIntoLines(words, medianHeight)
        val columns = detectColumns(lines, medianHeight)
        return DocumentGrid(
            rows = buildRows(lines, columns),
            columns = columns,
            averageConfidence = document.averageConfidence,
        )
    }

    private fun medianHeight(words: List<RecognizedWord>): Float {
        val heights = words.map { it.box.height }.filter { it > 0f }.sorted()
        if (heights.isEmpty()) return FALLBACK_HEIGHT
        return heights[heights.size / 2]
    }

    /**
     * Sweeps [words] top-to-bottom, cutting a new line whenever a word's
     * vertical centre sits further than [LINE_TOLERANCE] word heights below
     * the running centre of the line being built. Each line's words are then
     * merged into cells left-to-right.
     */
    fun groupIntoLines(words: List<RecognizedWord>, medianHeight: Float): List<List<Cell>> {
        val tolerance = LINE_TOLERANCE * medianHeight
        val sorted = words.sortedBy { it.box.centerY }

        val lines = mutableListOf<MutableList<RecognizedWord>>()
        var currentCenter = Float.NaN
        for (word in sorted) {
            val center = word.box.centerY
            if (lines.isEmpty() || center - currentCenter > tolerance) {
                lines.add(mutableListOf(word))
                currentCenter = center
            } else {
                val line = lines.last()
                line.add(word)
                // A running mean keeps a slightly sloped line (a photographed
                // page is never perfectly straight) from drifting out of its
                // own tolerance halfway across the page.
                currentCenter = line.sumOf { it.box.centerY.toDouble() }.toFloat() / line.size
            }
        }

        return lines.map { mergeIntoCells(it, medianHeight) }
    }

    /** Glues words of one line into cells, splitting wherever the gap exceeds [CELL_GAP]. */
    private fun mergeIntoCells(line: List<RecognizedWord>, medianHeight: Float): List<Cell> {
        val maxGap = CELL_GAP * medianHeight
        val sorted = line.sortedBy { it.box.left }

        val cells = mutableListOf<Cell>()
        var text = StringBuilder()
        var box: BoundingBox? = null
        var confidenceSum = 0f
        var count = 0

        fun flush() {
            val currentBox = box ?: return
            cells.add(Cell(text.toString(), currentBox, confidenceSum / count))
            text = StringBuilder()
            box = null
            confidenceSum = 0f
            count = 0
        }

        for (word in sorted) {
            val current = box
            if (current != null && word.box.left - current.right > maxGap) flush()
            if (box == null) {
                box = word.box
                text.append(word.text)
            } else {
                box = box!!.union(word.box)
                text.append(' ').append(word.text)
            }
            confidenceSum += word.confidence
            count++
        }
        flush()

        return cells
    }

    /**
     * Merges the horizontal extents of every cell into bands, so that what is
     * left between them are the whitespace corridors the layout used to
     * separate columns.
     *
     * Only lines that already hold at least two cells take part: a line the
     * eye reads as a single run of text (a title, an address, a total written
     * across the page) carries no column information and would bridge
     * corridors that the tabular rows keep clear.
     */
    fun detectColumns(lines: List<List<Cell>>, medianHeight: Float): List<ColumnBand> {
        val corridor = COLUMN_CORRIDOR * medianHeight
        val candidates = lines
            .filter { it.size >= 2 }
            .flatten()
            .filter { it.box.width <= WIDE_CELL_FRACTION }
            .sortedBy { it.box.left }

        if (candidates.isEmpty()) return listOf(ColumnBand(0f, 1f))

        val bands = mutableListOf<ColumnBand>()
        var left = candidates.first().box.left
        var right = candidates.first().box.right
        for (cell in candidates.drop(1)) {
            if (cell.box.left - right > corridor) {
                bands.add(ColumnBand(left, right))
                left = cell.box.left
            }
            right = maxOf(right, cell.box.right)
        }
        bands.add(ColumnBand(left, right))
        return bands
    }

    /**
     * Projects every line onto [columns], producing a rectangular grid. Cells
     * whose centre falls outside all bands (an over-wide title, a stray mark)
     * attach to the nearest one, and two cells landing in the same band on the
     * same line are joined with a space rather than one overwriting the other.
     */
    fun buildRows(lines: List<List<Cell>>, columns: List<ColumnBand>): List<List<String>> {
        if (columns.isEmpty()) return emptyList()

        return lines.mapNotNull { line ->
            val row = MutableList(columns.size) { "" }
            for (cell in line) {
                val index = columnIndexFor(cell, columns)
                row[index] = if (row[index].isEmpty()) cell.text else "${row[index]} ${cell.text}"
            }
            row.takeIf { cells -> cells.any { it.isNotBlank() } }
        }
    }

    private fun columnIndexFor(cell: Cell, columns: List<ColumnBand>): Int {
        val center = cell.box.centerX
        val containing = columns.indexOfFirst { it.contains(center) }
        if (containing >= 0) return containing
        return columns.indices.minByOrNull { columns[it].distanceTo(center) } ?: 0
    }
}
