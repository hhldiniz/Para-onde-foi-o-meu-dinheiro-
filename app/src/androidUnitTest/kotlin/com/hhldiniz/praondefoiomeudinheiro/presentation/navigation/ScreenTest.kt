package com.hhldiniz.praondefoiomeudinheiro.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Unit tests for the [Screen] navigation sealed class.
 *
 * Verifies that each route string is unique, non-blank, and accessible via the
 * [Screen.route] property.
 */
class ScreenTest {

    @Test
    fun landingRoute_isCorrect() {
        assertEquals("landing", Screen.Landing.route)
    }

    @Test
    fun homeRoute_isCorrect() {
        assertEquals("home", Screen.Home.route)
    }

    @Test
    fun settingsRoute_isCorrect() {
        assertEquals("settings", Screen.Settings.route)
    }

    @Test
    fun addEntryRoute_isCorrect() {
        assertEquals("add_entry", Screen.AddEntry.route)
    }

    @Test
    fun allRoutes_areUnique() {
        val routes = listOf(
            Screen.Landing.route,
            Screen.Home.route,
            Screen.Settings.route,
            Screen.AddEntry.route,
        )
        assertEquals("Expected 4 unique routes", 4, routes.toSet().size)
    }

    @Test
    fun allRoutes_areNonBlank() {
        listOf(Screen.Landing, Screen.Home, Screen.Settings, Screen.AddEntry).forEach {
            assert(it.route.isNotBlank()) { "Route for $it should not be blank" }
        }
    }

    @Test
    fun screenObjects_areNotEqual_toDifferentObjects() {
        assertNotEquals(Screen.Landing, Screen.Home)
        assertNotEquals(Screen.Home, Screen.Settings)
        assertNotEquals(Screen.Settings, Screen.AddEntry)
    }

    @Test
    fun sameObject_equalsItself() {
        assertEquals(Screen.Landing, Screen.Landing)
        assertEquals(Screen.Home, Screen.Home)
        assertEquals(Screen.Settings, Screen.Settings)
        assertEquals(Screen.AddEntry, Screen.AddEntry)
    }
}
