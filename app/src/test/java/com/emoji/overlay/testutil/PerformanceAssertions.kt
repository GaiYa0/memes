package com.emoji.overlay.testutil

import org.junit.Assert.assertTrue

object PerformanceAssertions {
    private val budgetMultiplier: Double = when {
        System.getenv("GITHUB_ACTIONS") == "true" -> 20.0
        System.getenv("CI") == "true" -> 20.0
        else -> 1.0
    }

    fun assertWithinMillis(label: String, elapsedMs: Double, localBudgetMs: Double) {
        val budgetMs = localBudgetMs * budgetMultiplier
        assertTrue(
            "$label should be < ${budgetMs}ms (elapsed=${"%.3f".format(elapsedMs)}ms)",
            elapsedMs < budgetMs
        )
    }
}
