package com.hhldiniz.praondefoiomeudinheiro.data.local

import com.hhldiniz.praondefoiomeudinheiro.data.local.prefs.KeyValueStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that persists whether the user has completed the initial
 * onboarding flow (starting patrimony + categories), independent of whether
 * they've added any [com.hhldiniz.praondefoiomeudinheiro.data.local.entity.ImportedEntry]
 * yet. `AppNavigation`'s start-destination check used to key purely off
 * `ImportRepository.count()`, which sent a user who finished onboarding but
 * hasn't added an entry straight back to onboarding on every reload/restart,
 * even though their patrimony/categories were correctly persisted.
 */
object OnboardingHolder {
    const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_COMPLETED = "completed"

    private val _completed = MutableStateFlow(false)
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    private var store: KeyValueStore? = null

    /** Initialises the holder from persisted storage; should be called once at app start. */
    fun init(store: KeyValueStore) {
        this.store = store
        _completed.value = store.getString(KEY_COMPLETED) == "true"
    }

    /** Marks onboarding as finished and persists it. */
    fun markCompleted() {
        _completed.value = true
        store?.putString(KEY_COMPLETED, "true")
    }

    /**
     * Forgets that onboarding was completed, so the next start destination is
     * the intro flow again. Used when the user clears all app data: with the
     * database emptied, keeping this flag would drop them on an empty Home
     * screen instead of taking them through onboarding again.
     */
    fun reset() {
        _completed.value = false
        store?.putString(KEY_COMPLETED, "false")
    }
}
