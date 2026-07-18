package com.hhldiniz.praondefoiomeudinheiro.data.local

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object CsvParser {

    fun parse(inputStream: InputStream): List<List<String>> {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val rows = mutableListOf<List<String>>()

        reader.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            rows.add(parseLine(line))
        }

        return rows
    }

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
