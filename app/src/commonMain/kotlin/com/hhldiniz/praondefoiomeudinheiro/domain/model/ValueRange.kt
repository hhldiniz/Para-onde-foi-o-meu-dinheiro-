package com.hhldiniz.praondefoiomeudinheiro.domain.model

/**
 * A single parsed row from a spreadsheet, with raw string fields for date,
 * amount, description and category before type conversion.
 */
data class CsvEntry(
    val date: String,
    val amount: String,
    val description: String,
    val category: String,
)

/**
 * Represents a range of rows read from a spreadsheet file, split into
 * spending and earnings entries. The raw [rows] list preserves the full
 * cell data for further processing.
 */
data class ValueRange(
    val range: String,
    val rows: List<List<String>>,
    val spendingEntries: List<CsvEntry> = emptyList(),
    val earningsEntries: List<CsvEntry> = emptyList(),
)
