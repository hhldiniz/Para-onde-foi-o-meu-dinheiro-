package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.addentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import com.hhldiniz.praondefoiomeudinheiro.data.repository.ImportRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AddEntryUiState(
    val dateMillis: Long = System.currentTimeMillis(),
    val amountText: String = "",
    val description: String = "",
    val category: String = "",
    val isExpense: Boolean = true,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null,
    val categories: List<String> = emptyList(),
    val showAddCategoryDialog: Boolean = false,
    val newCategoryName: String = "",
)

class AddEntryViewModel(
    private val importRepository: ImportRepository,
    private val categoryRepository: CategoryRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
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
            _uiState.value = state.copy(errorMessage = "Valor invalido")
            return
        }
        if (state.description.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Descricao obrigatoria")
            return
        }
        if (state.category.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Categoria obrigatoria")
            return
        }

        _uiState.value = state.copy(isSaving = true, errorMessage = null)

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
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
