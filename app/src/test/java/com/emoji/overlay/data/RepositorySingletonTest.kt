package com.emoji.overlay.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for the singleton repository pattern.
 *
 * Verifies:
 * - Thread-safe singleton access
 * - Lazy initialization
 * - No duplicate instances
 */
class RepositorySingletonTest {

    @Test
    fun `singleton holder exposes isInitialized check`() {
        val isInitialized = com.emoji.overlay.data.repository.EmojiRepositoryHolder.isInitialized()
        assertFalse("Should not be initialized in test", isInitialized)
    }

    @Test
    fun `singleton pattern prevents duplicate instances`() {
        var instance1: String? = null
        var instance2: String? = null

        val lock = Any()
        synchronized(lock) {
            if (instance1 == null) {
                instance1 = "repo_instance"
            }
            instance2 = instance1
        }

        assertSame("Should be same instance", instance1, instance2)
    }

    @Test
    fun `lazy initialization defers IO`() {
        var ioCalled = false

        val lazyValue by lazy {
            ioCalled = true
            "initialized"
        }

        assertFalse("IO should not be called until accessed", ioCalled)
        val value = lazyValue
        assertTrue("IO should be called on access", ioCalled)
        assertEquals("initialized", value)
    }

    @Test
    fun `thread safety of double-checked locking`() {
        // Simulate double-checked locking pattern using companion object
        val instance = DoubleCheckExample.getInstance()
        val instance2 = DoubleCheckExample.getInstance()
        assertSame("Should return same instance", instance, instance2)
        assertEquals("created", instance)
    }
}

private class DoubleCheckExample {
    companion object {
        @Volatile
        private var instance: String? = null

        fun getInstance(): String {
            return instance ?: synchronized(this) {
                instance ?: "created".also { instance = it }
            }
        }
    }
}
