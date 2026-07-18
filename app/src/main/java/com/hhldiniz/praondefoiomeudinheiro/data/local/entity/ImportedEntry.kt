package com.hhldiniz.praondefoiomeudinheiro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "imported_entries",
    indices = [
        Index(
            value = ["date_millis", "amount", "description", "category", "is_expense"],
            unique = true
        )
    ]
)
data class ImportedEntry(
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
    val importedAt: Long = System.currentTimeMillis(),
)
