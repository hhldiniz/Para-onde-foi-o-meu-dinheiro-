package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.investments

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.InvestmentDao
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Investment
import com.hhldiniz.praondefoiomeudinheiro.data.repository.InvestmentRepository
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvestmentType
import com.hhldiniz.praondefoiomeudinheiro.domain.model.YieldMode
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_error_invalid_current_value
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_error_invalid_invested_amount
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_error_invalid_yield_rate
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_error_name_required
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [InvestmentsViewModel], driven through a real
 * [InvestmentRepository] over an in-memory DAO: the tab's totals and
 * allocation are derived from what the DAO's Flow emits, so a fake that
 * actually re-emits on write exercises more of that path than a mock would.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InvestmentsViewModelTest {

    @get:Rule
    val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    /** Mirrors [InvestmentDao]'s contract, ordering included. */
    private class FakeInvestmentDao(initial: List<Investment> = emptyList()) : InvestmentDao {
        private val state = MutableStateFlow(initial)
        private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

        private fun List<Investment>.sorted() =
            sortedWith(compareByDescending<Investment> { it.dateMillis }.thenByDescending { it.id })

        override fun getAll(): Flow<List<Investment>> = state.map { it.sorted() }

        override suspend fun getAllSync(): List<Investment> = state.value.sorted()

        override suspend fun insert(investment: Investment): Long {
            val id = nextId++
            state.value = state.value + investment.copy(id = id)
            return id
        }

        override suspend fun update(investment: Investment) {
            state.value = state.value.map { if (it.id == investment.id) investment else it }
        }

        override suspend fun deleteById(id: Long) {
            state.value = state.value.filterNot { it.id == id }
        }

        override suspend fun deleteAll() {
            state.value = emptyList()
        }

        override suspend fun count(): Int = state.value.size

        fun current(): List<Investment> = state.value
    }

    private lateinit var dao: FakeInvestmentDao
    private lateinit var viewModel: InvestmentsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dao = FakeInvestmentDao()
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    private fun buildViewModel(initial: List<Investment> = emptyList()): InvestmentsViewModel {
        dao = FakeInvestmentDao(initial)
        return InvestmentsViewModel(InvestmentRepository(dao), testDispatcher)
    }

    private fun investment(
        id: Long = 0L,
        name: String = "Tesouro Selic",
        type: InvestmentType = InvestmentType.TREASURY,
        invested: Double = 1_000.0,
        current: Double = 1_100.0,
        dateMillis: Long = 1_700_000_000_000L,
    ) = Investment(
        id = id,
        name = name,
        type = type,
        investedAmount = invested,
        currentValue = current,
        dateMillis = dateMillis,
    )

    // -------------------------------------------------------------------------
    // Portfolio state
    // -------------------------------------------------------------------------

    @Test
    fun initialState_isEmptyPortfolio() = runTest {
        viewModel = buildViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.investments.isEmpty())
        assertEquals(0.0, state.totalInvested, 0.0)
        assertEquals(0.0, state.totalCurrent, 0.0)
        assertEquals(0.0, state.totalProfitPercent, 0.0)
        assertNull(state.form)
    }

    @Test
    fun storedInvestments_produceTotalsAndProfit() = runTest {
        viewModel = buildViewModel(
            listOf(
                investment(id = 1, invested = 1_000.0, current = 1_200.0),
                investment(id = 2, name = "PETR4", type = InvestmentType.STOCKS, invested = 1_000.0, current = 800.0),
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2_000.0, state.totalInvested, 0.001)
        assertEquals(2_000.0, state.totalCurrent, 0.001)
        assertEquals(0.0, state.totalProfit, 0.001)
        assertEquals(0.0, state.totalProfitPercent, 0.001)
    }

    @Test
    fun allocation_groupsByTypeBiggestFirst() = runTest {
        viewModel = buildViewModel(
            listOf(
                investment(id = 1, type = InvestmentType.TREASURY, current = 300.0),
                investment(id = 2, type = InvestmentType.STOCKS, current = 700.0),
                investment(id = 3, type = InvestmentType.TREASURY, current = 100.0),
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val allocation = viewModel.uiState.value.allocation
        assertEquals(listOf(InvestmentType.STOCKS, InvestmentType.TREASURY), allocation.map { it.type })
        assertEquals(700.0, allocation[0].value, 0.001)
        assertEquals(400.0, allocation[1].value, 0.001)
    }

    // -------------------------------------------------------------------------
    // Form
    // -------------------------------------------------------------------------

    @Test
    fun onAddClicked_opensBlankForm() = runTest {
        viewModel = buildViewModel()
        viewModel.onAddClicked()

        val form = viewModel.uiState.value.form
        assertNotNull(form)
        assertEquals("", form!!.name)
        assertFalse(form.isEditing)
    }

    @Test
    fun onEditClicked_prefillsFormWithPosition() = runTest {
        val stored = investment(id = 7, name = "CDB Banco X", type = InvestmentType.CDB, invested = 500.0, current = 540.5)
        viewModel = buildViewModel(listOf(stored))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEditClicked(stored)

        val form = viewModel.uiState.value.form!!
        assertTrue(form.isEditing)
        assertEquals(7L, form.id)
        assertEquals("CDB Banco X", form.name)
        assertEquals(InvestmentType.CDB, form.type)
        assertEquals("500", form.investedAmountText)
        assertEquals("540.5", form.currentValueText)
    }

    @Test
    fun save_blankName_setsError() = runTest {
        viewModel = buildViewModel()
        viewModel.onAddClicked()
        viewModel.onInvestedAmountChanged("100")
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(Res.string.investment_error_name_required, viewModel.uiState.value.form?.errorMessageRes)
        assertTrue(dao.current().isEmpty())
    }

    @Test
    fun save_invalidInvestedAmount_setsError() = runTest {
        viewModel = buildViewModel()
        viewModel.onAddClicked()
        viewModel.onNameChanged("Tesouro Selic")
        viewModel.onInvestedAmountChanged("abc")
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            Res.string.investment_error_invalid_invested_amount,
            viewModel.uiState.value.form?.errorMessageRes,
        )
        assertTrue(dao.current().isEmpty())
    }

    @Test
    fun save_negativeInvestedAmount_setsError() = runTest {
        viewModel = buildViewModel()
        viewModel.onAddClicked()
        viewModel.onNameChanged("Tesouro Selic")
        viewModel.onInvestedAmountChanged("-100")
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            Res.string.investment_error_invalid_invested_amount,
            viewModel.uiState.value.form?.errorMessageRes,
        )
    }

    @Test
    fun save_invalidCurrentValue_setsError() = runTest {
        viewModel = buildViewModel()
        viewModel.onAddClicked()
        viewModel.onNameChanged("Tesouro Selic")
        viewModel.onInvestedAmountChanged("100")
        viewModel.onCurrentValueChanged("???")
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            Res.string.investment_error_invalid_current_value,
            viewModel.uiState.value.form?.errorMessageRes,
        )
    }

    @Test
    fun save_validForm_insertsAndClosesForm() = runTest {
        viewModel = buildViewModel()
        viewModel.onAddClicked()
        viewModel.onNameChanged("  Tesouro Selic 2029  ")
        viewModel.onTypeChanged(InvestmentType.TREASURY)
        viewModel.onInstitutionChanged("Corretora")
        viewModel.onInvestedAmountChanged("1.234,56")
        viewModel.onCurrentValueChanged("1.300,00")
        viewModel.onDateChanged(1_700_000_000_000L)
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.form)
        assertFalse(viewModel.uiState.value.isSaving)

        val stored = dao.current().single()
        assertEquals("Tesouro Selic 2029", stored.name)
        assertEquals(1_234.56, stored.investedAmount, 0.001)
        assertEquals(1_300.0, stored.currentValue, 0.001)
        assertEquals(1_700_000_000_000L, stored.dateMillis)
        assertEquals(1, viewModel.uiState.value.investments.size)
    }

    @Test
    fun save_blankCurrentValue_readsAsInvestedAmount() = runTest {
        viewModel = buildViewModel()
        viewModel.onAddClicked()
        viewModel.onNameChanged("CDB")
        viewModel.onInvestedAmountChanged("2000")
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = dao.current().single()
        assertEquals(2_000.0, stored.currentValue, 0.001)
        assertEquals(0.0, stored.profit, 0.001)
    }

    @Test
    fun save_whileEditing_updatesInsteadOfInserting() = runTest {
        val stored = investment(id = 1, name = "CDB", invested = 500.0, current = 500.0)
        viewModel = buildViewModel(listOf(stored))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEditClicked(stored)
        viewModel.onCurrentValueChanged("620,40")
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        val positions = dao.current()
        assertEquals(1, positions.size)
        assertEquals(1L, positions.single().id)
        assertEquals(620.40, positions.single().currentValue, 0.001)
        assertEquals(620.40, viewModel.uiState.value.totalCurrent, 0.001)
    }

    @Test
    fun formEdits_afterDismiss_areIgnored() = runTest {
        viewModel = buildViewModel()
        viewModel.onAddClicked()
        viewModel.onFormDismissed()
        viewModel.onNameChanged("Ignored")

        assertNull(viewModel.uiState.value.form)
    }

    // -------------------------------------------------------------------------
    // Deletion
    // -------------------------------------------------------------------------

    @Test
    fun deleteClicked_armsConfirmationWithoutDeleting() = runTest {
        viewModel = buildViewModel(listOf(investment(id = 3)))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeleteClicked(3L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3L, viewModel.uiState.value.pendingDeleteId)
        assertEquals(1, dao.current().size)
    }

    @Test
    fun confirmDelete_removesPosition() = runTest {
        viewModel = buildViewModel(listOf(investment(id = 3), investment(id = 4)))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeleteClicked(3L)
        viewModel.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingDeleteId)
        assertEquals(listOf(4L), dao.current().map { it.id })
        assertEquals(1, viewModel.uiState.value.investments.size)
    }

    @Test
    fun deleteDismissed_keepsPosition() = runTest {
        viewModel = buildViewModel(listOf(investment(id = 3)))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onDeleteClicked(3L)
        viewModel.onDeleteDismissed()
        viewModel.confirmDelete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, dao.current().size)
    }

    // -------------------------------------------------------------------------
    // Contracted yield (fixed income)
    // -------------------------------------------------------------------------

    @Test
    fun save_storesTheContractedYield() = runTest {
        viewModel = buildViewModel()
        viewModel.onAddClicked()
        viewModel.onNameChanged("CDB Banco X")
        viewModel.onTypeChanged(InvestmentType.CDB)
        viewModel.onInvestedAmountChanged("1000")
        viewModel.onYieldModeChanged(YieldMode.CDI_PERCENT)
        viewModel.onYieldRateChanged("110")
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = dao.current().single()
        assertEquals(YieldMode.CDI_PERCENT, stored.yieldMode)
        assertEquals(110.0, stored.yieldRate!!, 0.001)
        assertTrue(stored.hasYield)
    }

    /** The rate goes through the amount parser, so both decimal conventions work, "%" and all. */
    @Test
    fun save_readsTheRateInEitherDecimalConventionAndWithAPercentSign() = runTest {
        viewModel = buildViewModel()
        viewModel.onAddClicked()
        viewModel.onNameChanged("Tesouro IPCA+ 2035")
        viewModel.onInvestedAmountChanged("1000")
        viewModel.onYieldModeChanged(YieldMode.IPCA_PLUS)
        viewModel.onYieldRateChanged(" 5,75 % ")
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(5.75, dao.current().single().yieldRate!!, 0.001)
    }

    @Test
    fun save_modeWithoutAReadableRate_setsError() = runTest {
        viewModel = buildViewModel()
        viewModel.onAddClicked()
        viewModel.onNameChanged("CDB Banco X")
        viewModel.onTypeChanged(InvestmentType.CDB)
        viewModel.onInvestedAmountChanged("1000")
        viewModel.onYieldModeChanged(YieldMode.CDI_PERCENT)
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            Res.string.investment_error_invalid_yield_rate,
            viewModel.uiState.value.form?.errorMessageRes,
        )
        assertTrue(dao.current().isEmpty())
    }

    @Test
    fun save_withoutAYieldMode_storesNone() = runTest {
        viewModel = buildViewModel()
        viewModel.onAddClicked()
        viewModel.onNameChanged("Tesouro Selic")
        viewModel.onInvestedAmountChanged("1000")
        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        val stored = dao.current().single()
        assertEquals(YieldMode.NONE, stored.yieldMode)
        assertNull(stored.yieldRate)
    }

    /** A variable-income position has no contracted rate, so switching to one drops what was typed. */
    @Test
    fun onTypeChanged_toVariableIncome_clearsTheYield() = runTest {
        viewModel = buildViewModel()
        viewModel.onAddClicked()
        viewModel.onNameChanged("PETR4")
        viewModel.onInvestedAmountChanged("1000")
        viewModel.onYieldModeChanged(YieldMode.CDI_PERCENT)
        viewModel.onYieldRateChanged("110")
        viewModel.onTypeChanged(InvestmentType.STOCKS)

        val form = viewModel.uiState.value.form!!
        assertFalse(form.showsYieldFields)
        assertEquals(YieldMode.NONE, form.yieldMode)
        assertEquals("", form.yieldRateText)

        viewModel.save()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(YieldMode.NONE, dao.current().single().yieldMode)
        assertNull(dao.current().single().yieldRate)
    }

    @Test
    fun onYieldModeChanged_toNone_clearsTheRate() = runTest {
        viewModel = buildViewModel()
        viewModel.onAddClicked()
        viewModel.onYieldModeChanged(YieldMode.SELIC_PLUS)
        viewModel.onYieldRateChanged("2")
        viewModel.onYieldModeChanged(YieldMode.NONE)

        val form = viewModel.uiState.value.form!!
        assertEquals("", form.yieldRateText)
        assertFalse(form.showsYieldRate)
    }

    @Test
    fun onEditClicked_prefillsTheContractedYield() = runTest {
        val stored = investment(id = 9, type = InvestmentType.CDB)
            .copy(yieldMode = YieldMode.CDI_PLUS, yieldRate = 1.5)
        viewModel = buildViewModel(listOf(stored))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEditClicked(stored)

        val form = viewModel.uiState.value.form!!
        assertEquals(YieldMode.CDI_PLUS, form.yieldMode)
        assertEquals("1.5", form.yieldRateText)
        assertTrue(form.showsYieldRate)
    }
}
