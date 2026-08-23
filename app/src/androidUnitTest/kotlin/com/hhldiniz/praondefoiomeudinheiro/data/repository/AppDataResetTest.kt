package com.hhldiniz.praondefoiomeudinheiro.data.repository

import com.hhldiniz.praondefoiomeudinheiro.data.local.DataClearedHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.OnboardingHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.PatrimonyHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.SelectedFilesHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.CategoryDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.prefs.InMemoryKeyValueStore
import com.hhldiniz.praondefoiomeudinheiro.domain.file.InMemoryPlatformFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Unit tests for [clearAllAppData], the single step behind the settings
 * screen's "clear all data" action: it has to leave the app in the same state
 * a first launch finds it in, so the user goes through onboarding again.
 *
 * The holders it touches are Kotlin objects (singletons), so each test starts
 * them from a fresh [InMemoryKeyValueStore] and restores them afterwards.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppDataResetTest {

    private val categoryDao: CategoryDao = mock()
    private val importRepository: ImportRepository = mock()

    @Before
    fun setUp() {
        PatrimonyHolder.init(InMemoryKeyValueStore())
        OnboardingHolder.init(InMemoryKeyValueStore())
        DataClearedHolder.reset()
        SelectedFilesHolder.files = emptyList()
    }

    @After
    fun tearDown() {
        PatrimonyHolder.init(InMemoryKeyValueStore())
        OnboardingHolder.init(InMemoryKeyValueStore())
        DataClearedHolder.reset()
        SelectedFilesHolder.files = emptyList()
    }

    @Test
    fun clearAllAppData_clearsEntriesAndCategories() = runTest {
        clearAllAppData(importRepository, categoryDao)

        verify(importRepository).clearAllData(categoryDao)
    }

    @Test
    fun clearAllAppData_forgetsSelectedFiles() = runTest {
        SelectedFilesHolder.files = listOf(InMemoryPlatformFile("extrato.csv"))

        clearAllAppData(importRepository, categoryDao)

        assertTrue(SelectedFilesHolder.files.isEmpty())
    }

    @Test
    fun clearAllAppData_resetsPatrimony() = runTest {
        PatrimonyHolder.setPatrimony(4_200.0)

        clearAllAppData(importRepository, categoryDao)

        assertEquals(0.0, PatrimonyHolder.patrimony.value, 0.0)
    }

    @Test
    fun clearAllAppData_sendsUserThroughOnboardingAgain() = runTest {
        OnboardingHolder.markCompleted()

        clearAllAppData(importRepository, categoryDao)

        assertFalse(OnboardingHolder.completed.value)
    }

    @Test
    fun clearAllAppData_marksDataAsCleared() = runTest {
        clearAllAppData(importRepository, categoryDao)

        assertTrue(DataClearedHolder.cleared.value)
    }
}
