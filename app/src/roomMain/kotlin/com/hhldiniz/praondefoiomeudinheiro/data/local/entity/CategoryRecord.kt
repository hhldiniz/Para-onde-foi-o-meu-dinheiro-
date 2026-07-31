package com.hhldiniz.praondefoiomeudinheiro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room persistence record for [Category], kept schema-identical to the original table. */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
)

fun Category.toRecord(): CategoryRecord = CategoryRecord(id = id, name = name)

fun CategoryRecord.toDomain(): Category = Category(id = id, name = name)
