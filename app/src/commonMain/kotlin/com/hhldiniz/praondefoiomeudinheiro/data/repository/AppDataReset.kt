package com.hhldiniz.praondefoiomeudinheiro.data.repository

import com.hhldiniz.praondefoiomeudinheiro.data.local.DataClearedHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.OnboardingHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.PatrimonyHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.SelectedFilesHolder
import com.hhldiniz.praondefoiomeudinheiro.data.local.dao.CategoryDao

/**
 * Takes the app back to its first-run state: entries and categories are
 * dropped, the files the landing flow picked are forgotten, and the starting
 * patrimony and the "onboarding is done" flag are cleared so the user goes
 * through onboarding again — both immediately (the settings screen navigates
 * to the intro) and on the next launch (`AppNavigation` picks its start
 * destination from the same two signals).
 *
 * [DataClearedHolder] is marked last so any screen still alive shows zeroed
 * data rather than falling back to whatever it had loaded.
 */
suspend fun clearAllAppData(
    importRepository: ImportRepository,
    categoryDao: CategoryDao,
) {
    importRepository.clearAllData(categoryDao)
    SelectedFilesHolder.files = emptyList()
    PatrimonyHolder.reset()
    OnboardingHolder.reset()
    DataClearedHolder.markCleared()
}
