package com.hhldiniz.praondefoiomeudinheiro.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.CategoryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.ImportedEntryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Category
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for [AppDatabase].
 *
 * Creates an in-memory Room database so tests are fast, isolated, and
 * leave no disk state behind. Covers both [ImportedEntryDao] and
 * [CategoryDao].
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var entryDao: ImportedEntryDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        entryDao = db.importedEntryDao()
        categoryDao = db.categoryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun expense(
        dateMillis: Long = 1_000_000L,
        amount: Double = 100.0,
        description: String = "Test",
        category: String = "Alimentacao",
        fileName: String = "f.csv",
    ) = ImportedEntry(
        dateMillis = dateMillis,
        amount = amount,
        description = description,
        category = category,
        isExpense = true,
        fileName = fileName,
    )

    private fun earning(
        dateMillis: Long = 2_000_000L,
        amount: Double = 1000.0,
        description: String = "Salary",
        category: String = "Salario",
        fileName: String = "f.csv",
    ) = ImportedEntry(
        dateMillis = dateMillis,
        amount = amount,
        description = description,
        category = category,
        isExpense = false,
        fileName = fileName,
    )

    // =========================================================================
    // ImportedEntryDao
    // =========================================================================

    @Test
    fun insertAll_returnsRowIds() = runTest {
        val entries = listOf(expense(), earning())
        val ids = entryDao.insertAll(entries)
        assertEquals(2, ids.size)
        assertTrue(ids.all { it > 0 })
    }

    @Test
    fun insertAll_duplicateIgnoredByConflictStrategy() = runTest {
        val e = expense()
        entryDao.insertAll(listOf(e))
        val ids2 = entryDao.insertAll(listOf(e))
        // Duplicate → rowId should be -1
        assertEquals(-1L, ids2[0])
    }

    @Test
    fun getAllEntries_returnsAllInserted() = runTest {
        entryDao.insertAll(listOf(expense(), earning()))
        val all = entryDao.getAllEntries().first()
        assertEquals(2, all.size)
    }

    @Test
    fun getAllEntries_orderedByDateDesc() = runTest {
        val older  = expense(dateMillis = 1_000_000L)
        val newer  = expense(dateMillis = 3_000_000L, description = "Newer", amount = 50.0)
        entryDao.insertAll(listOf(older, newer))
        val result = entryDao.getAllEntries().first()
        assertEquals(3_000_000L, result[0].dateMillis)
        assertEquals(1_000_000L, result[1].dateMillis)
    }

    @Test
    fun getSpendingEntries_returnsOnlyExpenses() = runTest {
        entryDao.insertAll(listOf(expense(), earning()))
        val spending = entryDao.getSpendingEntries().first()
        assertEquals(1, spending.size)
        assertTrue(spending.all { it.isExpense })
    }

    @Test
    fun getEarningsEntries_returnsOnlyEarnings() = runTest {
        entryDao.insertAll(listOf(expense(), earning()))
        val earnings = entryDao.getEarningsEntries().first()
        assertEquals(1, earnings.size)
        assertTrue(earnings.all { !it.isExpense })
    }

    @Test
    fun count_returnsCorrectNumber() = runTest {
        assertEquals(0, entryDao.count())
        entryDao.insertAll(listOf(expense(), earning()))
        assertEquals(2, entryDao.count())
    }

    @Test
    fun deleteAll_removesAllEntries() = runTest {
        entryDao.insertAll(listOf(expense(), earning()))
        entryDao.deleteAll()
        assertEquals(0, entryDao.count())
    }

    @Test
    fun getAllEntriesByDate_returnsNonFlow() = runTest {
        entryDao.insertAll(listOf(expense(), earning()))
        val all = entryDao.getAllEntriesByDate()
        assertEquals(2, all.size)
    }

    @Test
    fun getCategoryTotals_expenseFilter_returnsCorrectTotals() = runTest {
        val e1 = expense(amount = 100.0, category = "Alimentacao", dateMillis = 500L)
        val e2 = expense(amount = 200.0, category = "Alimentacao", description = "d2", dateMillis = 600L)
        val e3 = expense(amount = 50.0, category = "Transporte", dateMillis = 500L)
        entryDao.insertAll(listOf(e1, e2, e3))

        val totals = entryDao.getCategoryTotals(
            isExpense = true, category = null,
            startMillis = 0L, endMillis = Long.MAX_VALUE,
        )
        assertEquals(2, totals.size)
        val alimentacaoTotal = totals.find { it.category == "Alimentacao" }?.total
        assertEquals(300.0, alimentacaoTotal ?: 0.0, 0.001)
    }

    @Test
    fun getCategoryTotals_withCategoryFilter_returnsOnlyThatCategory() = runTest {
        val e1 = expense(amount = 100.0, category = "Alimentacao", dateMillis = 500L)
        val e2 = expense(amount = 50.0, category = "Transporte", dateMillis = 500L)
        entryDao.insertAll(listOf(e1, e2))

        val totals = entryDao.getCategoryTotals(
            isExpense = true, category = "Alimentacao",
            startMillis = 0L, endMillis = Long.MAX_VALUE,
        )
        assertEquals(1, totals.size)
        assertEquals("Alimentacao", totals[0].category)
    }

    @Test
    fun getCategoryTotals_dateRange_excludesOutOfRange() = runTest {
        val e1 = expense(amount = 100.0, dateMillis = 1000L)
        val e2 = expense(amount = 200.0, description = "Late", dateMillis = 9999L)
        entryDao.insertAll(listOf(e1, e2))

        val totals = entryDao.getCategoryTotals(
            isExpense = true, category = null,
            startMillis = 0L, endMillis = 2000L,
        )
        assertEquals(1, totals.size)
        assertEquals(100.0, totals[0].total, 0.001)
    }

    @Test
    fun getEntriesByDateRange_returnsOnlyMatchingEntries() = runTest {
        val e1 = expense(dateMillis = 1000L, description = "In range")
        val e2 = expense(dateMillis = 9999L, description = "Out of range", amount = 200.0)
        entryDao.insertAll(listOf(e1, e2))

        val result = entryDao.getEntriesByDateRange(
            isExpense = true, category = null,
            startMillis = 0L, endMillis = 5000L,
        )
        assertEquals(1, result.size)
        assertEquals("In range", result[0].description)
    }

    @Test
    fun getMinDate_returnsMinimumDate() = runTest {
        entryDao.insertAll(listOf(
            expense(dateMillis = 3000L),
            expense(dateMillis = 1000L, description = "d2"),
            expense(dateMillis = 2000L, description = "d3"),
        ))
        assertEquals(1000L, entryDao.getMinDate())
    }

    @Test
    fun getMaxDate_returnsMaximumDate() = runTest {
        entryDao.insertAll(listOf(
            expense(dateMillis = 1000L),
            expense(dateMillis = 3000L, description = "d2"),
            expense(dateMillis = 2000L, description = "d3"),
        ))
        assertEquals(3000L, entryDao.getMaxDate())
    }

    @Test
    fun getMinDate_emptyTable_returnsNull() = runTest {
        assertNull(entryDao.getMinDate())
    }

    @Test
    fun getMaxDate_emptyTable_returnsNull() = runTest {
        assertNull(entryDao.getMaxDate())
    }

    // =========================================================================
    // CategoryDao
    // =========================================================================

    @Test
    fun insert_category_returnsRowId() = runTest {
        val id = categoryDao.insert(Category(name = "Alimentacao"))
        assertTrue(id > 0)
    }

    @Test
    fun insert_duplicateCategoryIgnored() = runTest {
        categoryDao.insert(Category(name = "Alimentacao"))
        val id2 = categoryDao.insert(Category(name = "Alimentacao"))
        assertEquals(-1L, id2)
    }

    @Test
    fun getAll_returnsInsertedCategories() = runTest {
        categoryDao.insert(Category(name = "Lazer"))
        categoryDao.insert(Category(name = "Alimentacao"))
        val all = categoryDao.getAll().first()
        assertEquals(2, all.size)
        // Ordered by name ASC
        assertEquals("Alimentacao", all[0].name)
        assertEquals("Lazer", all[1].name)
    }

    @Test
    fun getAllSync_returnsInsertedCategories() = runTest {
        categoryDao.insert(Category(name = "Saude"))
        val all = categoryDao.getAllSync()
        assertEquals(1, all.size)
        assertEquals("Saude", all[0].name)
    }

    @Test
    fun insertAll_insertsMultipleCategories() = runTest {
        val cats = listOf(
            Category(name = "Cat1"),
            Category(name = "Cat2"),
            Category(name = "Cat3"),
        )
        categoryDao.insertAll(cats)
        val all = categoryDao.getAllSync()
        assertEquals(3, all.size)
    }

    @Test
    fun deleteAll_removesAllCategories() = runTest {
        categoryDao.insertAll(listOf(Category(name = "X"), Category(name = "Y")))
        categoryDao.deleteAll()
        assertTrue(categoryDao.getAllSync().isEmpty())
    }

    @Test
    fun categories_haveAutoGeneratedIds() = runTest {
        val id1 = categoryDao.insert(Category(name = "A"))
        val id2 = categoryDao.insert(Category(name = "B"))
        assertTrue(id1 > 0)
        assertTrue(id2 > 0)
        // IDs should be different
        assertTrue(id1 != id2)
    }

    // =========================================================================
    // AppDatabase – getInstance singleton
    // =========================================================================

    @Test
    fun getInstance_returnsSameInstance() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db1 = AppDatabase.getInstance(context)
        val db2 = AppDatabase.getInstance(context)
        assertEquals(db1, db2)
    }
}
