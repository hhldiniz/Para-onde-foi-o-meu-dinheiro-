package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.addentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import com.hhldiniz.praondefoiomeudinheiro.platform.currentTimeMillis
import com.hhldiniz.praondefoiomeudinheiro.resources.Res
import com.hhldiniz.praondefoiomeudinheiro.resources.add_entry_error_category_required
import com.hhldiniz.praondefoiomeudinheiro.resources.add_entry_error_description_required
import com.hhldiniz.praondefoiomeudinheiro.resources.add_entry_error_invalid_amount
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import com.hhldiniz.praondefoiomeudinheiro.platform.ioDispatcher as platformIoDispatcher

data class AddEntryUiState(
    val dateMillis: Long = currentTimeMillis(),
    val amountText: String = "",
    val description: String = "",
    val category: String = "",
    val isExpense: Boolean = true,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessageRes: StringResource? = null,
    val categories: List<String> = emptyList(),
    val showAddCategoryDialog: Boolean = false,
    val newCategoryName: String = "",
)

class AddEntryViewModel(
    private val importRepository: ImportRepository,
    private val categoryRepository: CategoryRepository,
    private val ioDispatcher: CoroutineDispatcher = platformIoDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEntryUiState())
    val uiState: StateFlow<AddEntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.getAll().collect { cats ->
                _uiState.value = _uiState.value.copy(
                    categories = cats.map { it.name }
                )
            }
        }
    }

    fun onDateChanged(dateMillis: Long) {
        _uiState.value = _uiState.value.copy(dateMillis = dateMillis)
    }

    fun onAmountChanged(amount: String) {
        _uiState.value = _uiState.value.copy(amountText = amount)
    }

    fun onDescriptionChanged(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun onCategoryChanged(category: String) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun onTypeChanged(isExpense: Boolean) {
        _uiState.value = _uiState.value.copy(isExpense = isExpense)
    }

    fun onShowAddCategoryDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(
            showAddCategoryDialog = show,
            newCategoryName = if (show) "" else _uiState.value.newCategoryName,
        )
    }

    fun onNewCategoryNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(newCategoryName = name)
    }

    fun addNewCategory() {
        val name = _uiState.value.newCategoryName.trim()
        if (name.isBlank()) return
        _uiState.value = _uiState.value.copy(isSaving = true)
        viewModelScope.launch {
            withContext(ioDispatcher) {
                categoryRepository.insert(name)
            }
            _uiState.value = _uiState.value.copy(
                category = name,
                showAddCategoryDialog = false,
                newCategoryName = "",
                isSaving = false,
            )
        }
    }

    fun save() {
        val state = _uiState.value
        val amount = state.amountText.replace(",", ".").toDoubleOrNull()
        if (amount == null) {
            _uiState.value = state.copy(errorMessageRes = Res.string.add_entry_error_invalid_amount)
            return
        }
        if (state.description.isBlank()) {
            _uiState.value = state.copy(errorMessageRes = Res.string.add_entry_error_description_required)
            return
        }
        if (state.category.isBlank()) {
            _uiState.value = state.copy(errorMessageRes = Res.string.add_entry_error_category_required)
            return
        }

        _uiState.value = state.copy(isSaving = true, errorMessageRes = null)

        viewModelScope.launch {
            val entry = ImportedEntry(
                dateMillis = state.dateMillis,
                amount = amount,
                description = state.description,
                category = state.category,
                isExpense = state.isExpense,
                fileName = "manual",
            )
            withContext(ioDispatcher) {
                importRepository.insertEntries(listOf(entry))
                categoryRepository.insert(state.category.trim())
            }
            _uiState.value = _uiState.value.copy(isSaving = false, savedSuccessfully = true)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessageRes = null)
    }
}
