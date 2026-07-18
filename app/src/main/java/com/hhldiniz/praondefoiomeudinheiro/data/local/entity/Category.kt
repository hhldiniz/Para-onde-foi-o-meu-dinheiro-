package com.hhldiniz.praondefoiomeudinheiro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
)

fun defaultCategories(): List<Category> = listOf(
    Category(name = "Alimentacao"),
    Category(name = "Transporte"),
    Category(name = "Lazer"),
    Category(name = "Saude"),
    Category(name = "Educacao"),
    Category(name = "Moradia"),
    Category(name = "Salario"),
    Category(name = "Freelance"),
    Category(name = "Investimentos"),
    Category(name = "Outros"),
)
