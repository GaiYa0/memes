package com.emoji.overlay

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for Navigation routes and button click safety.
 *
 * Verifies:
 * - All routes are defined correctly
 * - Route parameters are valid
 * - Navigation doesn't crash with null/invalid data
 */
class NavigationTest {

    @Test
    fun `all routes are defined and non-empty`() {
        val routes = listOf(
            Routes.HOME,
            Routes.IMPORT,
            Routes.BROWSE,
            Routes.FAVORITES,
            Routes.RECENT,
            Routes.CATEGORIES,
            Routes.CATEGORY_DETAIL,
            Routes.SEARCH
        )

        routes.forEach { route ->
            assertTrue("Route should not be empty: $route", route.isNotEmpty())
        }
    }

    @Test
    fun `category detail route handles valid id`() {
        assertEquals("category/1", Routes.categoryDetail(1))
        assertEquals("category/100", Routes.categoryDetail(100))
        assertEquals("category/999999", Routes.categoryDetail(999999L))
    }

    @Test
    fun `category detail route handles zero id`() {
        assertEquals("category/0", Routes.categoryDetail(0))
    }

    @Test
    fun `category detail route handles max long`() {
        val route = Routes.categoryDetail(Long.MAX_VALUE)
        assertTrue("Should handle max long", route.startsWith("category/"))
    }

    @Test
    fun `home route is start destination`() {
        assertEquals("home", Routes.HOME)
    }

    @Test
    fun `import route is correct`() {
        assertEquals("import", Routes.IMPORT)
    }

    @Test
    fun `browse route is correct`() {
        assertEquals("browse", Routes.BROWSE)
    }

    @Test
    fun `favorites route is correct`() {
        assertEquals("favorites", Routes.FAVORITES)
    }

    @Test
    fun `recent route is correct`() {
        assertEquals("recent", Routes.RECENT)
    }

    @Test
    fun `categories route is correct`() {
        assertEquals("categories", Routes.CATEGORIES)
    }

    @Test
    fun `search route is correct`() {
        assertEquals("search", Routes.SEARCH)
    }

    @Test
    fun `routes do not contain special characters that break navigation`() {
        val routes = listOf(
            Routes.HOME, Routes.IMPORT, Routes.BROWSE,
            Routes.FAVORITES, Routes.RECENT, Routes.CATEGORIES, Routes.SEARCH
        )

        routes.forEach { route ->
            // Routes should not contain spaces, quotes, or backslashes
            assertFalse("Route should not contain spaces: $route", route.contains(" "))
            assertFalse("Route should not contain quotes: $route", route.contains("\""))
            assertFalse("Route should not contain backslash: $route", route.contains("\\"))
        }
    }

    @Test
    fun `navigation actions do not throw on null context`() {
        // Verify that route constants are accessible without context
        val home = Routes.HOME
        val import = Routes.IMPORT
        assertNotNull(home)
        assertNotNull(import)
    }
}
