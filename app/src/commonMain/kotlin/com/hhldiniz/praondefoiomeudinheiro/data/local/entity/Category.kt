package com.hhldiniz.praondefoiomeudinheiro.data.local.entity

import kotlinx.serialization.Serializable

/**
 * A spending/earning category name. Room-free so it can be shared with
 * wasmJs; the Room-backed persistence for Android/iOS lives in `roomMain` as
 * `CategoryRecord`.
 */
@Serializable
data class Category(
    val id: Long = 0,
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
