package com.hhldiniz.praondefoiomeudinheiro.data.local

import com.hhldiniz.praondefoiomeudinheiro.data.local.prefs.InMemoryKeyValueStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [OnboardingHolder].
 *
 * OnboardingHolder is a Kotlin object (singleton). We reset it via
 * [OnboardingHolder.init] with a fresh, empty store after each test to avoid
 * test-order dependencies, and use an in-memory [InMemoryKeyValueStore] in
 * place of the platform's preferences.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingHolderTest {

    private lateinit var store: InMemoryKeyValueStore

    @Before
    fun setUp() {
        store = InMemoryKeyValueStore()
    }

    @After
    fun tearDown() {
        OnboardingHolder.init(InMemoryKeyValueStore())
    }

    @Test
    fun init_withNoSavedPreference_defaultsToFalse() = runTest {
        OnboardingHolder.init(store)
        assertFalse(OnboardingHolder.completed.value)
    }

    @Test
    fun init_withSavedCompletedFlag_restoresTrue() = runTest {
        OnboardingHolder.init(InMemoryKeyValueStore(mapOf("completed" to "true")))
        assertTrue(OnboardingHolder.completed.value)
    }

    @Test
    fun markCompleted_updatesStateFlow() = runTest {
        OnboardingHolder.init(store)
        OnboardingHolder.markCompleted()
        assertTrue(OnboardingHolder.completed.value)
    }

    @Test
    fun markCompleted_flowEmitsNewValue() = runTest {
        OnboardingHolder.init(store)
        OnboardingHolder.markCompleted()
        assertTrue(OnboardingHolder.completed.first())
    }

    @Test
    fun markCompleted_persistsAcrossReinit() = runTest {
        OnboardingHolder.init(store)
        OnboardingHolder.markCompleted()

        OnboardingHolder.init(store)
        assertTrue(OnboardingHolder.completed.value)
    }

    @Test
    fun markCompleted_canBeCalledMultipleTimes() = runTest {
        OnboardingHolder.init(store)
        OnboardingHolder.markCompleted()
        OnboardingHolder.markCompleted()
        assertTrue(OnboardingHolder.completed.value)
    }
}
