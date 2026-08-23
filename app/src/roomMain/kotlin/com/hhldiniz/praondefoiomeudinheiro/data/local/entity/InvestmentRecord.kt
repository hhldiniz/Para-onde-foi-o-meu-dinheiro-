package com.hhldiniz.praondefoiomeudinheiro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvestmentType

/**
 * Room persistence record for [Investment]. The type is stored as its stable
 * [InvestmentType.key] rather than the enum's ordinal or declared name, so
 * reordering or renaming an entry of that enum does not reinterpret rows
 * already on disk.
 */
@Entity(
    tableName = "investments",
    indices = [
        // Supports the list's ORDER BY date_millis DESC and the per-type totals.
        Index(value = ["date_millis"]),
        Index(value = ["type"]),
    ]
)
data class InvestmentRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "type")
    val typeKey: String,
    val institution: String = "",
    @ColumnInfo(name = "invested_amount")
    val investedAmount: Double,
    @ColumnInfo(name = "current_value")
    val currentValue: Double,
    @ColumnInfo(name = "date_millis")
    val dateMillis: Long,
    val notes: String = "",
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

fun Investment.toRecord(): InvestmentRecord = InvestmentRecord(
    id = id,
    name = name,
    typeKey = type.key,
    institution = institution,
    investedAmount = investedAmount,
    currentValue = currentValue,
    dateMillis = dateMillis,
    notes = notes,
    updatedAt = updatedAt,
)

fun InvestmentRecord.toDomain(): Investment = Investment(
    id = id,
    name = name,
    type = InvestmentType.fromKey(typeKey),
    institution = institution,
    investedAmount = investedAmount,
    currentValue = currentValue,
    dateMillis = dateMillis,
    notes = notes,
    updatedAt = updatedAt,
)
