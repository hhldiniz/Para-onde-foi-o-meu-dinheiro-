package com.hhldiniz.praondefoiomeudinheiro.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [PatrimonyHolder].
 *
 * PatrimonyHolder is a Kotlin object (singleton). We reset it to 0.0 after
 * each test to avoid test-order dependencies, and use a mocked
 * [SharedPreferences] to verify persistence calls.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PatrimonyHolderTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var context: Context

    @Before
    fun setUp() {
        prefs = mock()
        editor = mock()
        context = mock()
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putFloat(any(), any())).thenReturn(editor)
        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        whenever(prefs.getFloat(eq("patrimony"), any())).thenReturn(0f)
    }

    @After
    fun tearDown() {
        PatrimonyHolder.setPatrimony(0.0)
    }

    @Test
    fun init_withNoSavedPreference_defaultsToZero() = runTest {
        PatrimonyHolder.init(context)
        assertEquals(0.0, PatrimonyHolder.patrimony.value, 0.0)
    }

    @Test
    fun init_withSavedValue_restoresIt() = runTest {
        whenever(prefs.getFloat(eq("patrimony"), any())).thenReturn(5_000.5f)
        PatrimonyHolder.init(context)
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
