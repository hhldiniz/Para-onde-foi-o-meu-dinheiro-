package com.hhldiniz.praondefoiomeudinheiro.data.local

import com.hhldiniz.praondefoiomeudinheiro.data.local.prefs.InMemoryKeyValueStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PatrimonyHolder].
 *
 * PatrimonyHolder is a Kotlin object (singleton). We reset it to 0.0 after
 * each test to avoid test-order dependencies, and use an in-memory
 * [InMemoryKeyValueStore] in place of the platform's preferences.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PatrimonyHolderTest {

    private lateinit var store: InMemoryKeyValueStore

    @Before
    fun setUp() {
        store = InMemoryKeyValueStore()
    }

    @After
    fun tearDown() {
        PatrimonyHolder.setPatrimony(0.0)
    }

    @Test
    fun init_withNoSavedPreference_defaultsToZero() = runTest {
        PatrimonyHolder.init(store)
        assertEquals(0.0, PatrimonyHolder.patrimony.value, 0.0)
    }

    @Test
    fun init_withSavedValue_restoresIt() = runTest {
        PatrimonyHolder.init(InMemoryKeyValueStore(mapOf("patrimony" to 5_000.5)))
        assertEquals(5_000.5, PatrimonyHolder.patrimony.value, 0.01)
    }

    @Test
    fun setPatrimony_updatesStateFlow() = runTest {
        PatrimonyHolder.setPatrimony(1_234.0)
        assertEquals(1_234.0, PatrimonyHolder.patrimony.value, 0.0)
    }

    @Test
    fun setPatrimony_flowEmitsNewValue() = runTest {
        PatrimonyHolder.setPatrimony(999.0)
        assertEquals(999.0, PatrimonyHolder.patrimony.first(), 0.0)
    }

    @Test
    fun setPatrimony_canBeCalledMultipleTimes() = runTest {
        PatrimonyHolder.setPatrimony(100.0)
        assertEquals(100.0, PatrimonyHolder.patrimony.value, 0.0)
        PatrimonyHolder.setPatrimony(200.0)
        assertEquals(200.0, PatrimonyHolder.patrimony.value, 0.0)
    }
}
