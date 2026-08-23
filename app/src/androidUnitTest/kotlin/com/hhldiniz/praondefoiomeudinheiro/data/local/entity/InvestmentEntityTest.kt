package com.hhldiniz.praondefoiomeudinheiro.data.local.entity

import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvestmentClass
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvestmentType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [Investment]'s derived numbers and the two representations it is
 * stored in: the Room record (Android/iOS) and JSON (`localStorage` on
 * wasmJs). Both persist [InvestmentType] by its [InvestmentType.key], so a
 * changed key would silently reinterpret rows already on disk — hence the
 * key list is pinned here.
 */
class InvestmentEntityTest {

    private fun investment(invested: Double, current: Double) = Investment(
        name = "Tesouro Selic 2029",
        type = InvestmentType.TREASURY,
        investedAmount = invested,
        currentValue = current,
        dateMillis = 1_700_000_000_000L,
    )

    @Test
    fun profit_isCurrentValueMinusInvested() {
        assertEquals(150.0, investment(1_000.0, 1_150.0).profit, 0.001)
        assertEquals(-200.0, investment(1_000.0, 800.0).profit, 0.001)
    }

    @Test
    fun profitPercent_isRelativeToInvested() {
        assertEquals(15.0, investment(1_000.0, 1_150.0).profitPercent, 0.001)
        assertEquals(-20.0, investment(1_000.0, 800.0).profitPercent, 0.001)
    }

    @Test
    fun profitPercent_withNothingInvested_isZeroInsteadOfInfinite() {
        assertEquals(0.0, investment(0.0, 500.0).profitPercent, 0.001)
    }

    @Test
    fun roomRecord_roundTripsEveryField() {
        val original = Investment(
            id = 42,
            name = "PETR4",
            type = InvestmentType.STOCKS,
            institution = "Corretora",
            investedAmount = 1_234.56,
            currentValue = 1_300.0,
            dateMillis = 1_700_000_000_000L,
            notes = "20 cotas",
            updatedAt = 1_700_000_900_000L,
        )

        assertEquals(original, original.toRecord().toDomain())
        assertEquals("stocks", original.toRecord().typeKey)
    }

    @Test
    fun roomRecord_withUnknownTypeKey_readsBackAsOther() {
        val record = InvestmentRecord(
            id = 1,
            name = "Algo novo",
            typeKey = "type_from_a_newer_version",
            investedAmount = 10.0,
            currentValue = 10.0,
            dateMillis = 1_700_000_000_000L,
            updatedAt = 1_700_000_000_000L,
        )

        assertEquals(InvestmentType.OTHER, record.toDomain().type)
    }

    @Test
    fun json_roundTripsForWebStorage() {
        val original = investment(1_000.0, 1_150.0).copy(id = 3, notes = "vence em 2029")

        val decoded = Json.decodeFromString<Investment>(Json.encodeToString(original))

        assertEquals(original, decoded)
    }

    /**
     * `updatedAt` defaults to "now", and Json omits a property whose value
     * equals its default — a default that is re-evaluated at encode time, so
     * without `@EncodeDefault` a position built and stored in the same
     * millisecond went to `localStorage` without its timestamp and came back
     * carrying the load time instead.
     */
    @Test
    fun json_writesUpdatedAtEvenWhenItHoldsItsDefault() {
        val encoded = Json.encodeToString(
            Investment(
                name = "Tesouro Selic 2029",
                type = InvestmentType.TREASURY,
                investedAmount = 100.0,
                currentValue = 100.0,
                dateMillis = 1_700_000_000_000L,
            )
        )

        assertTrue(encoded, encoded.contains("\"updatedAt\""))
    }

    @Test
    fun json_serializesTypeByItsStableKey() {
        val encoded = Json.encodeToString(investment(1.0, 1.0).copy(type = InvestmentType.REAL_ESTATE_FUND))

        assertTrue(encoded, encoded.contains("\"real_estate_fund\""))
    }

    @Test
    fun investmentTypeKeys_areUniqueAndStable() {
        val keys = InvestmentType.entries.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
        assertEquals(
            listOf(
                "savings",
                "treasury",
                "cdb",
                "lci_lca",
                "cri_cra",
                "debenture",
                "fixed_income_fund",
                "stocks",
                "real_estate_fund",
                "etf",
                "bdr",
                "foreign_stocks",
                "multimarket_fund",
                "crypto",
                "commodities",
                "foreign_currency",
                "pension",
                "real_estate",
                "other",
            ),
            keys,
        )
    }

    @Test
    fun investmentTypeFromKey_resolvesKnownKeysAndFallsBack() {
        InvestmentType.entries.forEach { type ->
            assertEquals(type, InvestmentType.fromKey(type.key))
        }
        assertEquals(InvestmentType.OTHER, InvestmentType.fromKey("nao_existe"))
    }

    @Test
    fun everyAssetClass_hasAtLeastOneType() {
        InvestmentClass.entries.forEach { assetClass ->
            assertNotNull(
                assetClass.name,
                InvestmentType.entries.firstOrNull { it.assetClass == assetClass },
            )
        }
    }
}
