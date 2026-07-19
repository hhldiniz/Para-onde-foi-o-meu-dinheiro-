package com.hhldiniz.praondefoiomeudinheiro.data.local

import android.content.Context
import android.content.SharedPreferences
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
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
 * Unit tests for [CurrencyHolder].
 *
 * CurrencyHolder is a Kotlin object (singleton). We reset it to BRL after
 * each test to avoid test-order dependencies, and use a mocked
 * [SharedPreferences] to verify persistence calls.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyHolderTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var context: Context

    @Before
    fun setUp() {
        prefs = mock()
        editor = mock()
        context = mock()
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(context.getSharedPreferences(any(), any())).thenReturn(prefs)
        // Default: no previously saved currency
        whenever(prefs.getString(eq("selected_currency"), eq(null))).thenReturn(null)
    }

    @After
    fun tearDown() {
        // Reset singleton to default
        CurrencyHolder.setCurrency(CurrencyOption.BRL)
    }

    // -------------------------------------------------------------------------
    // init
    // -------------------------------------------------------------------------

    @Test
    fun init_withNoSavedPreference_defaultsToBrl() = runTest {
        whenever(prefs.getString(any(), eq(null))).thenReturn(null)
        CurrencyHolder.init(context)
        assertEquals(CurrencyOption.BRL, CurrencyHolder.selectedCurrency.value)
    }

    @Test
    fun init_withSavedEurCode_restoresEur() = runTest {
        whenever(prefs.getString(eq("selected_currency"), eq(null))).thenReturn("EUR")
        CurrencyHolder.init(context)
        assertEquals(CurrencyOption.EUR, CurrencyHolder.selectedCurrency.value)
    }

    @Test
    fun init_withSavedUsdCode_restoresUsd() = runTest {
        whenever(prefs.getString(eq("selected_currency"), eq(null))).thenReturn("USD")
        CurrencyHolder.init(context)
        assertEquals(CurrencyOption.USD, CurrencyHolder.selectedCurrency.value)
    }

    @Test
    fun init_withSavedGbpCode_restoresGbp() = runTest {
        whenever(prefs.getString(eq("selected_currency"), eq(null))).thenReturn("GBP")
        CurrencyHolder.init(context)
        assertEquals(CurrencyOption.GBP, CurrencyHolder.selectedCurrency.value)
    }

    @Test
    fun init_withSavedArsCode_restoresArs() = runTest {
        whenever(prefs.getString(eq("selected_currency"), eq(null))).thenReturn("ARS")
        CurrencyHolder.init(context)
        assertEquals(CurrencyOption.ARS, CurrencyHolder.selectedCurrency.value)
    }

    @Test
    fun init_withUnknownCode_keepsDefault() = runTest {
        whenever(prefs.getString(eq("selected_currency"), eq(null))).thenReturn("XYZ")
        // BRL is the current value; unknown code should not change it
        CurrencyHolder.setCurrency(CurrencyOption.BRL)
        CurrencyHolder.init(context)
        assertEquals(CurrencyOption.BRL, CurrencyHolder.selectedCurrency.value)
    }

    // -------------------------------------------------------------------------
    // setCurrency
    // -------------------------------------------------------------------------

    @Test
    fun setCurrency_updatesStateFlow() = runTest {
        CurrencyHolder.setCurrency(CurrencyOption.USD)
        assertEquals(CurrencyOption.USD, CurrencyHolder.selectedCurrency.value)
    }

    @Test
    fun setCurrency_flowEmitsNewValue() = runTest {
        CurrencyHolder.setCurrency(CurrencyOption.ARS)
        assertEquals(CurrencyOption.ARS, CurrencyHolder.selectedCurrency.first())
    }

    @Test
    fun setCurrency_canBeCalledMultipleTimes() = runTest {
        CurrencyHolder.setCurrency(CurrencyOption.USD)
        assertEquals(CurrencyOption.USD, CurrencyHolder.selectedCurrency.value)
        CurrencyHolder.setCurrency(CurrencyOption.EUR)
        assertEquals(CurrencyOption.EUR, CurrencyHolder.selectedCurrency.value)
        CurrencyHolder.setCurrency(CurrencyOption.BRL)
        assertEquals(CurrencyOption.BRL, CurrencyHolder.selectedCurrency.value)
    }

    @Test
    fun setCurrency_allOptionsCanBeSet() = runTest {
        CurrencyOption.entries.forEach { option ->
            CurrencyHolder.setCurrency(option)
            assertEquals(option, CurrencyHolder.selectedCurrency.value)
        }
    }
}
