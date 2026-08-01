package com.hhldiniz.praondefoiomeudinheiro.presentation.screen.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hhldiniz.praondefoiomeudinheiro.data.local.OnboardingHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.entity.defaultCategories
import com.hhldiniz.praondefoiomeudinheiro.data.repository.CategoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.hhldiniz.praondefoiomeudinheiro.platform.ioDispatcher as platformIoDispatcher

private val DEFAULT_CATEGORY_NAMES = defaultCategories().map { it.name }

/** UI state for the initial-categories introduction screen. */
data class IntroCategoriesUiState(
    val availableCategories: List<String> = DEFAULT_CATEGORY_NAMES,
    val selectedCategories: Set<String> = DEFAULT_CATEGORY_NAMES.toSet(),
    val newCategoryText: String = "",
    val confirmed: Boolean = false,
)

/**
 * ViewModel for the second onboarding screen, where the user picks which of
 * the suggested default categories to start with and can add their own. On
 * Android/iOS the suggested defaults are already seeded in Room on first
 * database creation (see `AppDatabase` in `roomMain`); `onContinue` inserts
 * every still-selected default anyway (a no-op there thanks to conflict-
 * ignore semantics), which is also what actually seeds them on wasmJs, which
 * has no such database-creation callback.
 */
class IntroCategoriesViewModel(
    private val categoryRepository: CategoryRepository,
    private val ioDispatcher: CoroutineDispatcher = platformIoDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IntroCategoriesUiState())
    val uiState: StateFlow<IntroCategoriesUiState> = _uiState.asStateFlow()

    fun onCategoryToggled(name: String) {
        _uiState.update { state ->
            val selected = if (name in state.selectedCategories) {
                state.selectedCategories - name
            } else {
                state.selectedCategories + name
            }
            state.copy(selectedCategories = selected)
        }
    }

    fun onNewCategoryTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(newCategoryText = text)
    }

    fun addCustomCategory() {
        val name = _uiState.value.newCategoryText.trim()
        if (name.isBlank() || name in _uiState.value.availableCategories) return
        _uiState.update { state ->
            state.copy(
                availableCategories = state.availableCategories + name,
                selectedCategories = state.selectedCategories + name,
                newCategoryText = "",
            )
        }
    }

    /** Removes deselected defaults and inserts the selected/custom categories, then signals completion. */
    fun onContinue() {
        val selected = _uiState.value.selectedCategories
        viewModelScope.launch {
            withContext(ioDispatcher) {
                DEFAULT_CATEGORY_NAMES.filter { it !in selected }
                    .forEach { categoryRepository.deleteByName(it) }
                categoryRepository.insertAll(selected.toList())
            }
            OnboardingHolder.markCompleted()
            _uiState.value = _uiState.value.copy(confirmed = true)
        }
    }
}
