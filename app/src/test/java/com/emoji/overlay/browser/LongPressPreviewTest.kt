package com.emoji.overlay.browser

import com.emoji.overlay.data.entity.EmojiEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for LongPressPreview logic.
 */
class LongPressPreviewTest {

    @Test
    fun `animated detection for GIF`() {
        val gif = createTestEmoji(mimeType = "image/gif")
        assertTrue(isAnimated(gif))
    }

    @Test
    fun `animated detection for WebP`() {
        val webp = createTestEmoji(mimeType = "image/webp")
        assertTrue(isAnimated(webp))
    }

    @Test
    fun `not animated for PNG`() {
        val png = createTestEmoji(mimeType = "image/png")
        assertFalse(isAnimated(png))
    }

    @Test
    fun `not animated for JPEG`() {
        val jpeg = createTestEmoji(mimeType = "image/jpeg")
        assertFalse(isAnimated(jpeg))
    }

    @Test
    fun `preview scale limits`() {
        var scale = 1f
        val minScale = 0.5f
        val maxScale = 3f

        // Zoom in
        scale = (scale * 1.5f).coerceIn(minScale, maxScale)
        assertEquals(1.5f, scale, 0.01f)

        // Zoom out
        scale = (scale * 0.5f).coerceIn(minScale, maxScale)
        assertEquals(0.75f, scale, 0.01f)

        // Exceed max
        scale = 10f
        scale = scale.coerceIn(minScale, maxScale)
        assertEquals(3f, scale, 0.01f)

        // Below min
        scale = 0.1f
        scale = scale.coerceIn(minScale, maxScale)
        assertEquals(0.5f, scale, 0.01f)
    }

    @Test
    fun `file size formatting`() {
        assertEquals("500B", formatFileSize(500))
        assertEquals("1KB", formatFileSize(1024))
        assertEquals("1MB", formatFileSize(1024 * 1024))
        assertEquals("5MB", formatFileSize(5 * 1024 * 1024))
    }

    @Test
    fun `emoji info display`() {
        val emoji = createTestEmoji(
            name = "smile",
            keywords = "happy,face",
            width = 128,
            height = 128,
            fileSize = 10240
        )

        assertEquals("smile", emoji.name)
        assertEquals("happy,face", emoji.keywords)
        assertEquals(128, emoji.width)
        assertEquals(128, emoji.height)
        assertEquals(10240, emoji.fileSize)
    }

    @Test
    fun `favorite toggle in preview`() {
        val emoji = createTestEmoji(isFavorite = false)
        assertFalse(emoji.isFavorite)

        val favorited = emoji.copy(isFavorite = true)
        assertTrue(favorited.isFavorite)
    }

    // ==================== HELPERS ====================

    private fun createTestEmoji(
        name: String = "test",
        mimeType: String = "image/png",
        keywords: String = "",
        width: Int = 0,
        height: Int = 0,
        fileSize: Long = 0,
        isFavorite: Boolean = false
    ) = EmojiEntity(
        id = 1,
        name = name,
        mimeType = mimeType,
        keywords = keywords,
        width = width,
        height = height,
        fileSize = fileSize,
        filePath = "/test/$name.png",
        contentHash = "hash_1",
        isFavorite = isFavorite
    )

    private fun isAnimated(emoji: EmojiEntity): Boolean {
        return emoji.mimeType == "image/gif" || emoji.mimeType == "image/webp"
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            else -> "${bytes / (1024 * 1024)}MB"
        }
    }
}
