package com.hhldiniz.praondefoiomeudinheiro.data.local

import com.hhldiniz.praondefoiomeudinheiro.data.local.prefs.InMemoryKeyValueStore
import com.hhldiniz.praondefoiomeudinheiro.domain.model.CurrencyOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CurrencyHolder].
 *
 * CurrencyHolder is a Kotlin object (singleton). We reset it to BRL after
 * each test to avoid test-order dependencies, and use an in-memory
 * [InMemoryKeyValueStore] in place of the platform's preferences.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyHolderTest {

    private lateinit var store: InMemoryKeyValueStore

    @Before
    fun setUp() {
        // Default: no previously saved currency
        store = InMemoryKeyValueStore()
    }

    private fun storeWith(savedCode: String) = InMemoryKeyValueStore(
        mapOf("selected_currency" to savedCode)
    )

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
        CurrencyHolder.init(store)
        assertEquals(CurrencyOption.BRL, CurrencyHolder.selectedCurrency.value)
    }

    @Test
    fun init_withSavedEurCode_restoresEur() = runTest {
        CurrencyHolder.init(storeWith("EUR"))
        assertEquals(CurrencyOption.EUR, CurrencyHolder.selectedCurrency.value)
    }

    @Test
    fun init_withSavedUsdCode_restoresUsd() = runTest {
        CurrencyHolder.init(storeWith("USD"))
        assertEquals(CurrencyOption.USD, CurrencyHolder.selectedCurrency.value)
    }

    @Test
    fun init_withSavedGbpCode_restoresGbp() = runTest {
        CurrencyHolder.init(storeWith("GBP"))
        assertEquals(CurrencyOption.GBP, CurrencyHolder.selectedCurrency.value)
    }

    @Test
    fun init_withSavedArsCode_restoresArs() = runTest {
        CurrencyHolder.init(storeWith("ARS"))
        assertEquals(CurrencyOption.ARS, CurrencyHolder.selectedCurrency.value)
    }

    @Test
    fun init_withUnknownCode_keepsDefault() = runTest {
        // BRL is the current value; unknown code should not change it
        CurrencyHolder.setCurrency(CurrencyOption.BRL)
        CurrencyHolder.init(storeWith("XYZ"))
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
