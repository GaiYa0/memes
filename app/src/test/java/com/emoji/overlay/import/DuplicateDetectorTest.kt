package com.emoji.overlay.import

import com.emoji.overlay.import.util.DuplicateDetector
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

/**
 * Unit tests for DuplicateDetector.
 */
class DuplicateDetectorTest {

    @Test
    fun `calculate hash is deterministic`() {
        val data = "test content".toByteArray()
        val hash1 = calculateHash(data)
        val hash2 = calculateHash(data)
        assertEquals(hash1, hash2)
    }

    @Test
    fun `calculate hash differs for different content`() {
        val hash1 = calculateHash("content A".toByteArray())
        val hash2 = calculateHash("content B".toByteArray())
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `calculate hash format is SHA-256`() {
        val hash = calculateHash("test".toByteArray())
        // SHA-256 produces 64 hex characters
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `same content same hash regardless of filename`() {
        val content = "identical content for both files".toByteArray()
        val hash1 = calculateHash(content)
        val hash2 = calculateHash(content)
        assertEquals("Same content should produce same hash", hash1, hash2)
    }

    @Test
    fun `empty content produces valid hash`() {
        val hash = calculateHash(ByteArray(0))
        assertEquals(64, hash.length)
    }

    @Test
    fun `large content produces valid hash`() {
        val largeContent = ByteArray(1024 * 1024) { (it % 256).toByte() }
        val hash = calculateHash(largeContent)
        assertEquals(64, hash.length)
    }

    @Test
    fun `duplicate detection within batch`() {
        val contents = listOf(
            "content_a".toByteArray(),
            "content_b".toByteArray(),
            "content_a".toByteArray(),  // duplicate
            "content_c".toByteArray(),
            "content_b".toByteArray()   // duplicate
        )

        val hashes = contents.map { calculateHash(it) }
        val uniqueHashes = hashes.toSet()

        assertEquals(5, hashes.size)
        assertEquals(3, uniqueHashes.size) // 3 unique: a, b, c
    }

    // ==================== HELPERS ====================

    private fun calculateHash(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(data).joinToString("") { "%02x".format(it) }
    }
}
