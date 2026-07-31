package com.hhldiniz.praondefoiomeudinheiro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room persistence record for [ImportedEntry], kept schema-identical to the
 * table Room generated when this entity lived directly in commonMain. A
 * unique index on (date, amount, description, category, is_expense) prevents
 * duplicate imports.
 */
@Entity(
    tableName = "imported_entries",
    indices = [
        Index(
            value = ["date_millis", "amount", "description", "category", "is_expense"],
            unique = true
        ),
        // Supports WHERE is_expense = ? AND date_millis BETWEEN ? AND ? (category totals, chart data).
        Index(value = ["is_expense", "date_millis"]),
        // Supports WHERE date_millis BETWEEN ? AND ? without an is_expense filter (entries list paging).
        Index(value = ["date_millis"]),
    ]
)
data class ImportedEntryRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "date_millis")
    val dateMillis: Long,
    val amount: Double,
    val description: String,
    val category: String,
    @ColumnInfo(name = "is_expense")
    val isExpense: Boolean,
    @ColumnInfo(name = "file_name")
    val fileName: String = "",
    @ColumnInfo(name = "imported_at")
    val importedAt: Long,
)

fun ImportedEntry.toRecord(): ImportedEntryRecord = ImportedEntryRecord(
    id = id,
    dateMillis = dateMillis,
    amount = amount,
    description = description,
    category = category,
    isExpense = isExpense,
    fileName = fileName,
    importedAt = importedAt,
)

fun ImportedEntryRecord.toDomain(): ImportedEntry = ImportedEntry(
    id = id,
    dateMillis = dateMillis,
    amount = amount,
    description = description,
    category = category,
    isExpense = isExpense,
    fileName = fileName,
    importedAt = importedAt,
)
