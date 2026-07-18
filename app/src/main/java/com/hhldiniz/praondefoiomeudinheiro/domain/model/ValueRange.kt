package com.hhldiniz.praondefoiomeudinheiro.domain.model

data class CsvEntry(
    val date: String,
    val amount: String,
    val description: String,
    val category: String,
)

data class ValueRange(
    val range: String,
    val rows: List<List<String>>,
    val spendingEntries: List<CsvEntry> = emptyList(),
    val earningsEntries: List<CsvEntry> = emptyList(),
)
