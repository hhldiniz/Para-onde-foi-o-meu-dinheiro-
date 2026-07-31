package com.hhldiniz.praondefoiomeudinheiro.data.local

/** Simple CSV parser that turns raw file bytes into rows of strings. */
object CsvParser {

    /** Decodes [bytes] as UTF-8 and parses them; see [parse]. */
    fun parse(bytes: ByteArray): List<List<String>> = parse(bytes.decodeToString())

    /**
     * Parses the entire file content into a list of rows, skipping blank lines.
     * Each row is split into columns respecting standard CSV quoting rules.
     */
    fun parse(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()

        content.lineSequence().forEach { line ->
            if (line.isBlank()) return@forEach
            rows.add(parseLine(line))
        }

        return rows
    }

    /**
     * Parses a single CSV line into columns, handling double-quoted fields
     * and escaped quotes ("").
     */
    private fun parseLine(line: String): List<String> {
        val columns = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i += 2
                        continue
                    }
                    inQuotes = !inQuotes
                }
                c == ',' && !inQuotes -> {
                    columns.add(current.toString().trim())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }

        columns.add(current.toString().trim())
        return columns
    }
}
