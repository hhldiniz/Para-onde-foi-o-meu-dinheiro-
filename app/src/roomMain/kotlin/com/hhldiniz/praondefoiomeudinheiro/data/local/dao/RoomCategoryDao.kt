package com.hhldiniz.praondefoiomeudinheiro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Category
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.CategoryRecord
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.toDomain
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.toRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room DAO for [CategoryRecord] persistence; implements the Room-free [CategoryDao] contract. */
@Dao
interface RoomCategoryDao : CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllRecords(): Flow<List<CategoryRecord>>

    override fun getAll(): Flow<List<Category>> = getAllRecords().map { list -> list.map { it.toDomain() } }

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllSyncRecords(): List<CategoryRecord>

    override suspend fun getAllSync(): List<Category> = getAllSyncRecords().map { it.toDomain() }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecord(category: CategoryRecord): Long

    override suspend fun insert(category: Category): Long = insertRecord(category.toRecord())

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllRecords(categories: List<CategoryRecord>)

    override suspend fun insertAll(categories: List<Category>) =
        insertAllRecords(categories.map { it.toRecord() })

    @Query("DELETE FROM categories")
    override suspend fun deleteAll()

    @Query("DELETE FROM categories WHERE name = :name")
    override suspend fun deleteByName(name: String)
}
