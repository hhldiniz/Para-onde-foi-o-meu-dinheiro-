package com.hhldiniz.praondefoiomeudinheiro.data.local

/**
 * One positioned run of text on a PDF page, in PDF user-space units: [x] is
 * the run's left edge, [y] its baseline (growing *upwards*, unlike screen
 * coordinates), [width] its advance.
 */
data class PdfTextRun(
    val text: String,
    val x: Float,
    val y: Float,
    val width: Float,
)

/**
 * Rebuilds the whitespace-padded page text that [PdfParser.splitIntoRows]
 * splits into columns, from a PDF engine's raw positioned runs.
 *
 * Only the wasmJs build needs this: PDFBox (`sortByPosition = true`) on
 * Android and PDFKit's `.string` on iOS already pad their output, while
 * pdf.js's `getTextContent()` hands back the runs and nothing else. It lives
 * in common code all the same, so it is reachable from the JVM test run —
 * the same arrangement `data/local/zip/RawInflate.kt` has.
 */
object PdfTextLayout {

    /** Runs whose baselines are this close belong to the same visual line. */
    private const val LINE_TOLERANCE = 2f

    /**
     * Gap thresholds, in multiples of the line's own average character width.
     * They cannot be absolute PDF points: the same 8pt gap is a column
     * boundary in an 11pt statement and mid-word in a 24pt heading.
     */
    private const val COLUMN_GAP = 1.4f
    private const val WORD_GAP = 0.25f

    /** Used when a page carries no run wide enough to measure a character from. */
    private const val FALLBACK_CHAR_WIDTH = 4f

    /**
     * Returns the page as text, one line per visual row, with columns padded
     * out to the 2+ spaces [PdfParser.splitIntoRows] splits on.
     */
    fun reconstruct(runs: List<PdfTextRun>): String {
        val lines = groupIntoLines(runs)
        // PDF space grows upward, so reading order is descending y.
        lines.sortByDescending { it.baseline }

        val pageCharWidth = medianCharWidth(lines.flatMap { it.runs })

        return lines.joinToString("\n") { line ->
            // Prefer the line's own text size — a 7pt footer and a 14pt
            // heading do not share a column threshold — and fall back to the
            // page's when the line is too short to measure.
            val charWidth = medianCharWidth(line.runs)
                .takeIf { it > 0f }
                ?: pageCharWidth.takeIf { it > 0f }
                ?: FALLBACK_CHAR_WIDTH
            renderLine(line.runs.sortedBy { it.x }, charWidth)
        }
    }

    private class Line(val baseline: Float) {
        val runs = mutableListOf<PdfTextRun>()
    }

    private fun groupIntoLines(runs: List<PdfTextRun>): MutableList<Line> {
        val lines = mutableListOf<Line>()
        for (run in runs) {
            // Runs with no printable text are dropped rather than kept as
            // content. A PDF engine may report the space *between* two columns
            // as a run of its own — pdf.js reports it as a single space whose
            // width spans the entire gap — and keeping those would make every
            // gap measured below come out as zero: the columns would end up
            // one space apart and each row would parse as a single column.
            // Dropping them leaves the real distance between the printable
            // runs visible, which is what the padding is derived from.
            if (run.text.isBlank()) continue
            val line = lines.firstOrNull { kotlin.math.abs(it.baseline - run.y) <= LINE_TOLERANCE }
                ?: Line(run.y).also { lines += it }
            line.runs += run
        }
        return lines
    }

    private fun renderLine(runs: List<PdfTextRun>, charWidth: Float): String {
        val text = StringBuilder()
        var previousEnd: Float? = null
        for (run in runs) {
            val end = previousEnd
            if (end != null) {
                val gap = (run.x - end) / charWidth
                // A gap about a character wide is ordinary word spacing; a
                // wide one is very likely a column boundary in a tabular
                // report, so it is padded well past the two-space threshold.
                val spaces = when {
                    gap >= COLUMN_GAP -> maxOf(2, kotlin.math.round(gap).toInt())
                    gap >= WORD_GAP -> 1
                    else -> 0
                }
                repeat(spaces) { text.append(' ') }
            }
            text.append(run.text)
            previousEnd = run.x + run.width
        }
        return text.toString()
    }

    /** Rough width of one character, the unit every gap threshold is expressed in. */
    private fun medianCharWidth(runs: List<PdfTextRun>): Float {
        val widths = runs
            .filter { it.width > 0f && it.text.isNotEmpty() }
            .map { it.width / it.text.length }
            .sorted()
        return if (widths.isEmpty()) 0f else widths[widths.size / 2]
    }
}
