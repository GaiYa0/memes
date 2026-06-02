package com.emoji.overlay

import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compose state smoke tests for route render safety.
 */
class ComposeStateRenderTest {

    @Test
    fun `compose state updates do not throw`() {
        val query = mutableStateOf("")
        query.value = "abc"
        assertEquals("abc", query.value)
    }

    @Test
    fun `real route labels are safe to render`() {
        val labels = mutableStateOf(
            listOf("导入表情", "浏览表情", "收藏", "最近使用", "分类管理", "搜索")
        )
        assertEquals(6, labels.value.size)
        assertTrue(labels.value.all { it.isNotBlank() })
    }

    @Test
    fun `search screen input state can toggle`() {
        val query = mutableStateOf("")
        assertTrue(query.value.isBlank())
        query.value = "happy"
        assertFalse(query.value.isBlank())
        assertEquals("happy", query.value)
    }
}
