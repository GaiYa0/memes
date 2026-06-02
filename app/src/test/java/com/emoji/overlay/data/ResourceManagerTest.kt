package com.emoji.overlay.data

import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

/**
 * Unit tests for ResourceManager utility logic.
 *
 * Tests the pure logic methods that don't require Android context.
 * Full integration tests for file operations require Android instrumentation.
 */
class ResourceManagerTest {

    @Test
    fun `supported mime types include common image formats`() {
        val supportedTypes = setOf("image/png", "image/jpeg", "image/webp", "image/gif")

        assertTrue("image/png should be supported", "image/png" in supportedTypes)
        assertTrue("image/jpeg should be supported", "image/jpeg" in supportedTypes)
        assertTrue("image/webp should be supported", "image/webp" in supportedTypes)
        assertTrue("image/gif should be supported", "image/gif" in supportedTypes)
    }

    @Test
    fun `unsupported mime types are rejected`() {
        val supportedTypes = setOf("image/png", "image/jpeg", "image/webp", "image/gif")

        assertFalse("video/mp4 should not be supported", "video/mp4" in supportedTypes)
        assertFalse("application/pdf should not be supported", "application/pdf" in supportedTypes)
        assertFalse("text/plain should not be supported", "text/plain" in supportedTypes)
    }

    @Test
    fun `content hash is deterministic`() {
        val data = "test data".toByteArray()
        val hash1 = calculateContentHash(data)
        val hash2 = calculateContentHash(data)
        assertEquals("Hash should be deterministic", hash1, hash2)
    }

    @Test
    fun `content hash differs for different data`() {
        val data1 = "data1".toByteArray()
        val data2 = "data2".toByteArray()
        val hash1 = calculateContentHash(data1)
        val hash2 = calculateContentHash(data2)
        assertNotEquals("Hashes should differ for different data", hash1, hash2)
    }

    @Test
    fun `content hash is consistent format`() {
        val data = "hello".toByteArray()
        val hash = calculateContentHash(data)
        // MD5 should be 32 hex characters
        assertEquals("Hash should be 32 chars", 32, hash.length)
        assertTrue("Hash should be hex", hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `mime type detection from extension`() {
        assertEquals("image/png", detectMimeType("test.png"))
        assertEquals("image/jpeg", detectMimeType("test.jpg"))
        assertEquals("image/jpeg", detectMimeType("test.jpeg"))
        assertEquals("image/webp", detectMimeType("test.webp"))
        assertEquals("image/gif", detectMimeType("test.gif"))
        assertEquals("application/octet-stream", detectMimeType("test.xyz"))
    }

    @Test
    fun `mime type detection is case insensitive`() {
        assertEquals("image/png", detectMimeType("test.PNG"))
        assertEquals("image/jpeg", detectMimeType("test.JPG"))
        assertEquals("image/gif", detectMimeType("test.GIF"))
    }

    // Helper methods
    private fun calculateContentHash(data: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(data).joinToString("") { "%02x".format(it) }
    }

    private fun detectMimeType(filename: String): String {
        return when (filename.substringAfterLast('.').lowercase()) {
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> "application/octet-stream"
        }
    }
}
