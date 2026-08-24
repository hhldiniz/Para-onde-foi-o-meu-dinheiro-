package com.hhldiniz.praondefoiomeudinheiro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvestmentType
import com.hhldiniz.praondefoiomeudinheiro.domain.model.YieldMode

/**
 * Room persistence record for [Investment]. The type and the contracted
 * yield mode are stored as their stable [InvestmentType.key]/[YieldMode.key]
 * rather than the enums' ordinals or declared names, so reordering or
 * renaming an entry of either does not reinterpret rows already on disk.
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
    // Added in schema version 5 by an ALTER TABLE migration; the defaults
    // below are what a row written before it reads back as.
    @ColumnInfo(name = "yield_mode", defaultValue = "none")
    val yieldModeKey: String = YieldMode.NONE.key,
    @ColumnInfo(name = "yield_rate")
    val yieldRate: Double? = null,
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
    yieldModeKey = yieldMode.key,
    yieldRate = yieldRate,
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
    yieldMode = YieldMode.fromKey(yieldModeKey),
    yieldRate = yieldRate,
    updatedAt = updatedAt,
)
