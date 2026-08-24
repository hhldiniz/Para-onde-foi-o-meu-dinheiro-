package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.investments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.local.CurrencyHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.TransactionValueParser
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.Investment
import com.hhldiniz.praondefoiomeudinheiro.data.repository.InvestmentRepository
import com.hhldiniz.praondefoiomeudinheiro.domain.model.InvestmentType
import com.hhldiniz.praondefoiomeudinheiro.domain.model.YieldMode
import com.hhldiniz.praondefoiomeudinheiro.platform.currentTimeMillis
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_error_invalid_current_value
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_error_invalid_invested_amount
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_error_invalid_yield_rate
import com.hhldiniz.praondefoiomeudinheiro.resources.investment_error_name_required
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.hhldiniz.praondefoiomeudinheiro.platform.ioDispatcher as platformIoDispatcher

/**
 * Drives the investments tab. The list is collected straight from
 * [InvestmentRepository]'s Flow, so every insert/update/delete re-emits and
 * the totals below are recomputed from that same emission — there is no
 * separate refresh handshake like the one Home needs after AddEntry.
 *
 * Values are typed in by hand: nothing here quotes a market price, so a
 * position is worth whatever the user last said it was worth.
 */
class InvestmentsViewModel(
    private val repository: InvestmentRepository,
    private val ioDispatcher: CoroutineDispatcher = platformIoDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvestmentsUiState())
    val uiState: StateFlow<InvestmentsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAll().collect { investments ->
                _uiState.update { state ->
                    state.copy(
                        investments = investments,
                        totalInvested = investments.sumOf { it.investedAmount },
                        totalCurrent = investments.sumOf { it.currentValue },
                        allocation = allocationOf(investments),
                    )
                }
            }
        }
        viewModelScope.launch {
            CurrencyHolder.selectedCurrency.collect { currency ->
                _uiState.update { it.copy(selectedCurrency = currency) }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Form
    // ---------------------------------------------------------------------

    /** Opens an empty form. */
    fun onAddClicked() {
        _uiState.update { it.copy(form = InvestmentFormState()) }
    }

    /** Opens the form filled in with [investment], so saving updates that row. */
    fun onEditClicked(investment: Investment) {
        _uiState.update {
            it.copy(
                form = InvestmentFormState(
                    id = investment.id,
                    name = investment.name,
                    type = investment.type,
                    institution = investment.institution,
                    investedAmountText = formatAmount(investment.investedAmount),
                    currentValueText = formatAmount(investment.currentValue),
                    dateMillis = investment.dateMillis,
                    notes = investment.notes,
                    yieldMode = investment.yieldMode,
                    yieldRateText = investment.yieldRate?.let { rate -> formatAmount(rate) }.orEmpty(),
                )
            )
        }
    }

    fun onFormDismissed() {
        _uiState.update { it.copy(form = null) }
    }

    fun onNameChanged(name: String) = updateForm { it.copy(name = name, errorMessageRes = null) }

    /**
     * Switching to a type that carries no contracted rate (anything outside
     * fixed income) drops whatever yield was typed, so a hidden field cannot
     * be saved behind the user's back.
     */
    fun onTypeChanged(type: InvestmentType) = updateForm { form ->
        if (type.supportsYield) {
            form.copy(type = type)
        } else {
            form.copy(type = type, yieldMode = YieldMode.NONE, yieldRateText = "", errorMessageRes = null)
        }
    }

    fun onInstitutionChanged(institution: String) = updateForm { it.copy(institution = institution) }

    fun onInvestedAmountChanged(text: String) =
        updateForm { it.copy(investedAmountText = text, errorMessageRes = null) }

    fun onCurrentValueChanged(text: String) =
        updateForm { it.copy(currentValueText = text, errorMessageRes = null) }

    fun onDateChanged(dateMillis: Long) = updateForm { it.copy(dateMillis = dateMillis) }

    fun onNotesChanged(notes: String) = updateForm { it.copy(notes = notes) }

    /** Picking "não informado" also clears the rate, which would otherwise be unreachable but still saved. */
    fun onYieldModeChanged(mode: YieldMode) = updateForm { form ->
        form.copy(
            yieldMode = mode,
            yieldRateText = if (mode.hasRate) form.yieldRateText else "",
            errorMessageRes = null,
        )
    }

    fun onYieldRateChanged(text: String) =
        updateForm { it.copy(yieldRateText = text, errorMessageRes = null) }

    /**
     * Validates the open form and writes it, inserting or updating depending
     * on whether it carries an id. A blank "worth today" is read as "the same
     * as what I put in", which is what a position bought today is worth.
     */
    fun save() {
        val form = _uiState.value.form ?: return

        if (form.name.isBlank()) {
            updateForm { it.copy(errorMessageRes = Res.string.investment_error_name_required) }
            return
        }
        val invested = TransactionValueParser.parseAmount(form.investedAmountText)
        if (invested == null || invested < 0.0) {
            updateForm { it.copy(errorMessageRes = Res.string.investment_error_invalid_invested_amount) }
            return
        }
        val current = if (form.currentValueText.isBlank()) {
            invested
        } else {
            TransactionValueParser.parseAmount(form.currentValueText)
        }
        if (current == null || current < 0.0) {
            updateForm { it.copy(errorMessageRes = Res.string.investment_error_invalid_current_value) }
            return
        }

        // A mode with no readable rate would store "IPCA + ?", which says
        // nothing, so it is rejected rather than silently dropped.
        val yieldMode = if (form.showsYieldFields) form.yieldMode else YieldMode.NONE
        val yieldRate = if (yieldMode.hasRate) {
            val rate = parseRate(form.yieldRateText)
            if (rate == null || rate < 0.0) {
                updateForm { it.copy(errorMessageRes = Res.string.investment_error_invalid_yield_rate) }
                return
            }
            rate
        } else {
            null
        }

        val investment = Investment(
            id = form.id ?: 0L,
            name = form.name.trim(),
            type = form.type,
            institution = form.institution.trim(),
            investedAmount = invested,
            currentValue = current,
            dateMillis = form.dateMillis,
            notes = form.notes.trim(),
            yieldMode = yieldMode,
            yieldRate = yieldRate,
            updatedAt = currentTimeMillis(),
        )

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            withContext(ioDispatcher) {
                if (form.id == null) repository.insert(investment) else repository.update(investment)
            }
            _uiState.update { it.copy(isSaving = false, form = null) }
        }
    }

    // ---------------------------------------------------------------------
    // Deletion
    // ---------------------------------------------------------------------

    /** Arms the confirmation dialog for [id]; nothing is deleted until [confirmDelete]. */
    fun onDeleteClicked(id: Long) {
        _uiState.update { it.copy(pendingDeleteId = id) }
    }

    fun onDeleteDismissed() {
        _uiState.update { it.copy(pendingDeleteId = null) }
    }

    fun confirmDelete() {
        val id = _uiState.value.pendingDeleteId ?: return
        _uiState.update { it.copy(pendingDeleteId = null) }
        viewModelScope.launch {
            withContext(ioDispatcher) {
                repository.delete(id)
            }
        }
    }

    private fun updateForm(transform: (InvestmentFormState) -> InvestmentFormState) {
        _uiState.update { state ->
            val form = state.form ?: return@update state
            state.copy(form = transform(form))
        }
    }

    private companion object {

        /**
         * Current value per type, biggest slice first and types nobody holds
         * left out — the shape the pie chart expects.
         */
        fun allocationOf(investments: List<Investment>): List<TypeAllocation> =
            investments
                .groupBy { it.type }
                .map { (type, positions) -> TypeAllocation(type, positions.sumOf { it.currentValue }) }
                .filter { it.value > 0.0 }
                .sortedByDescending { it.value }

        /**
         * Reads a contracted rate. It goes through the same parser the
         * amounts do — so "1,5" and "1.5" both work — after dropping a
         * percent sign the user may well have typed along with it.
         */
        fun parseRate(text: String): Double? =
            TransactionValueParser.parseAmount(text.trim().removeSuffix("%").trim())

        /**
         * Renders a stored amount back into the form's text field. The parser
         * that reads it back accepts either decimal convention, so the plain
         * `Double` rendering is enough and avoids dragging a locale-aware
         * formatter into the ViewModel.
         */
        fun formatAmount(value: Double): String =
            if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    }
}
