package com.hhldiniz.praondefoiomeudinheiro.data.repository

import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.CategoryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Category
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {

    fun getAll(): Flow<List<Category>> = dao.getAll()

    suspend fun getAllSync(): List<Category> = dao.getAllSync()

    suspend fun insert(name: String): Long {
        return dao.insert(Category(name = name))
    }
}
