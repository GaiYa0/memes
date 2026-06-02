package com.emoji.overlay

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for home action button routing.
 *
 * These tests ensure each visible home button points to a valid route,
 * so click handling cannot crash due to invalid navigation target.
 */
class HomeButtonClickTest {

    @Test
    fun `import button route is valid`() {
        assertTrue(isValidTopLevelRoute(Routes.IMPORT))
    }

    @Test
    fun `browse button route is valid`() {
        assertTrue(isValidTopLevelRoute(Routes.BROWSE))
    }

    @Test
    fun `favorites button route is valid`() {
        assertTrue(isValidTopLevelRoute(Routes.FAVORITES))
    }

    @Test
    fun `recent button route is valid`() {
        assertTrue(isValidTopLevelRoute(Routes.RECENT))
    }

    @Test
    fun `categories button route is valid`() {
        assertTrue(isValidTopLevelRoute(Routes.CATEGORIES))
    }

    @Test
    fun `search button route is valid`() {
        assertTrue(isValidTopLevelRoute(Routes.SEARCH))
    }

    @Test
    fun `category detail route format stays stable`() {
        val route = Routes.categoryDetail(7L)
        assertTrue(route.startsWith("category/"))
    }
}
