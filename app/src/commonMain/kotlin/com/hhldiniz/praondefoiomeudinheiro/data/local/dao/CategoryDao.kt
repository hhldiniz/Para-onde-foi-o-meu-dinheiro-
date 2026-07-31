package com.hhldiniz.praondefoiomeudinheiro.data.local.dao

import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

/**
 * [Category] persistence contract. Room-backed on Android/iOS
 * (`RoomCategoryDao` in `roomMain`), localStorage-backed on wasmJs
 * (`WebCategoryDao`).
 */
interface CategoryDao {

    fun getAll(): Flow<List<Category>>

    suspend fun getAllSync(): List<Category>

    suspend fun insert(category: Category): Long

    suspend fun insertAll(categories: List<Category>)

    /** Deletes every row from the table. */
    suspend fun deleteAll()

    /** Deletes the category with the given [name], if any. */
    suspend fun deleteByName(name: String)
}
