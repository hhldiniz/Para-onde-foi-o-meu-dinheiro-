package com.hhldiniz.praondefoiomeudinheiro.data.local

import com.hhldiniz.praondefoiomeudinheiro.domain.model.CsvEntry
import com.hhldiniz.praondefoiomeudinheiro.util.normalizeForMatching

/**
 * Locates transaction columns (date, amount, description, category) inside a
 * header row by matching cell text against known name variants, instead of
 * assuming a fixed column order or a single language. This lets imported
 * files use any column order and recognizes Portuguese/English/Spanish
 * header names (plus a few common synonyms) for the same field.
 *
 * A header row can describe more than one side-by-side transaction table
 * (the app's own two-table Despesas/Renda export layout): every recognized
 * date column starts a new [ColumnGroup], spanning up to the next date
 * column or the end of the row. Groups alternate spending/earnings by
 * position (1st, 3rd, ... table = expenses; 2nd, 4th, ... = earnings),
 * matching that layout's convention.
 */
object TransactionColumnMapper {

    private val DATE_SYNONYMS = setOf("data", "date", "dia", "fecha", "day")
    private val AMOUNT_SYNONYMS = setOf("valor", "amount", "value", "montante", "importe", "preco")
    private val DESCRIPTION_SYNONYMS = setOf(
        "descricao", "description", "descripcion", "historico", "detalhe", "detalhes",
        "observacao", "obs", "memo", "note", "notes",
    )
    private val CATEGORY_SYNONYMS = setOf("categoria", "category", "tipo")

    /** A recognized transaction table within a header row. */
    data class ColumnGroup(
        val dateCol: Int,
        val amountCol: Int,
        val descriptionCol: Int?,
        val categoryCol: Int?,
        val isExpense: Boolean,
    )

    private fun matches(cell: String, synonyms: Set<String>) = normalizeForMatching(cell) in synonyms

    /** Finds the first row that contains at least one recognizable transaction table. */
    fun findHeaderRowIndex(rows: List<List<String>>): Int =
        rows.indexOfFirst { findColumnGroups(it).isNotEmpty() }

    /**
     * Scans [headerRow] for one or more transaction tables. A table requires
     * a recognizable date column and a recognizable amount column somewhere
     * before the next table's date column (or the end of the row);
     * description/category are optional and default to blank when absent.
     *
     * The first table's search range starts at column 0 — not at its date
     * column — so a field placed before the date (e.g. a leading category
     * column) is still found. Later tables search from their own date column
     * onward only, so they never re-claim a column already used by an
     * earlier table.
     */
    fun findColumnGroups(headerRow: List<String>): List<ColumnGroup> {
        val dateIndices = headerRow.indices.filter { matches(headerRow[it], DATE_SYNONYMS) }
        val groups = mutableListOf<ColumnGroup>()

        dateIndices.forEachIndexed { i, dateCol ->
            val searchStart = if (i == 0) 0 else dateCol
            val searchEnd = dateIndices.getOrElse(i + 1) { headerRow.size }
            val amountCol = (searchStart until searchEnd).firstOrNull { matches(headerRow[it], AMOUNT_SYNONYMS) } ?: return@forEachIndexed
            val descriptionCol = (searchStart until searchEnd).firstOrNull { matches(headerRow[it], DESCRIPTION_SYNONYMS) }
            val categoryCol = (searchStart until searchEnd).firstOrNull { matches(headerRow[it], CATEGORY_SYNONYMS) }
            groups.add(
                ColumnGroup(
                    dateCol = dateCol,
                    amountCol = amountCol,
                    descriptionCol = descriptionCol,
                    categoryCol = categoryCol,
                    isExpense = groups.size % 2 == 0,
                )
            )
        }

        return groups
    }

    /** Extracts a [CsvEntry] for [group] from [row], or null if its date/amount cells are missing or blank. */
    fun extractEntry(row: List<String>, group: ColumnGroup): CsvEntry? {
        if (group.dateCol >= row.size || group.amountCol >= row.size) return null
        val date = row[group.dateCol].trim()
        val amount = row[group.amountCol].trim()
        if (date.isBlank() || amount.isBlank()) return null

        val description = group.descriptionCol?.takeIf { it < row.size }?.let { row[it].trim() } ?: ""
        val category = group.categoryCol?.takeIf { it < row.size }?.let { row[it].trim() } ?: ""
        return CsvEntry(date = date, amount = amount, description = description, category = category)
    }
}
