package com.hhldiniz.praondefoiomeudinheiro.data.local

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DataClearedHolder].
 *
 * Because [DataClearedHolder] is a Kotlin object (singleton) we reset it
 * via [DataClearedHolder.reset] in [After] to prevent test-order dependencies.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DataClearedHolderTest {

    @After
    fun tearDown() {
        DataClearedHolder.reset()
    }

    @Test
    fun initialState_isFalse() = runTest {
        assertFalse(DataClearedHolder.cleared.value)
    }

    @Test
    fun markCleared_setsValueToTrue() = runTest {
        DataClearedHolder.markCleared()
        assertTrue(DataClearedHolder.cleared.value)
    }

    @Test
    fun reset_setsValueBackToFalse() = runTest {
        DataClearedHolder.markCleared()
        DataClearedHolder.reset()
        assertFalse(DataClearedHolder.cleared.value)
    }

    @Test
    fun cleared_flowEmitsCurrentValueOnCollection() = runTest {
        assertFalse(DataClearedHolder.cleared.first())
        DataClearedHolder.markCleared()
        assertTrue(DataClearedHolder.cleared.first())
    }

    @Test
    fun markCleared_canBeCalledMultipleTimes() = runTest {
        DataClearedHolder.markCleared()
        DataClearedHolder.markCleared()
        assertTrue(DataClearedHolder.cleared.value)
    }

    @Test
    fun reset_canBeCalledWhenAlreadyFalse() = runTest {
        DataClearedHolder.reset()
        assertFalse(DataClearedHolder.cleared.value)
    }
}
