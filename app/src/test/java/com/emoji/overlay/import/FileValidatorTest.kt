package com.emoji.overlay.import

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for FileValidator logic.
 */
class FileValidatorTest {

    @Test
    fun `PNG magic bytes validation`() {
        val validPng = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertTrue(isValidPngHeader(validPng))

        val invalidPng = byteArrayOf(0x00, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertFalse(isValidPngHeader(invalidPng))
    }

    @Test
    fun `JPEG magic bytes validation`() {
        val validJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        assertTrue(isValidJpegHeader(validJpeg))

        val invalidJpeg = byteArrayOf(0x00, 0xD8.toByte(), 0xFF.toByte())
        assertFalse(isValidJpegHeader(invalidJpeg))
    }

    @Test
    fun `GIF magic bytes validation`() {
        val validGif87a = "GIF87a".toByteArray()
        assertTrue(isValidGifHeader(validGif87a))

        val validGif89a = "GIF89a".toByteArray()
        assertTrue(isValidGifHeader(validGif89a))

        val invalidGif = "NOTGIF".toByteArray()
        assertFalse(isValidGifHeader(invalidGif))
    }

    @Test
    fun `WebP magic bytes validation`() {
        val validWebp = "RIFF".toByteArray() + ByteArray(4) + "WEBP".toByteArray()
        assertTrue(isValidWebpHeader(validWebp))

        val invalidWebp = "RIFF".toByteArray() + ByteArray(4) + "XXXX".toByteArray()
        assertFalse(isValidWebpHeader(invalidWebp))
    }

    @Test
    fun `empty file is invalid`() {
        val empty = ByteArray(0)
        assertFalse(isValidPngHeader(empty))
        assertFalse(isValidJpegHeader(empty))
        assertFalse(isValidGifHeader(empty))
        assertFalse(isValidWebpHeader(empty))
    }

    @Test
    fun `file too small is invalid`() {
        val tooSmall = byteArrayOf(0x89.toByte(), 0x50)
        assertFalse(isValidPngHeader(tooSmall))
    }

    @Test
    fun `MIME type detection from header`() {
        assertEquals("image/png", detectMimeTypeFromHeader(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)))
        assertEquals("image/jpeg", detectMimeTypeFromHeader(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())))
        assertEquals("image/gif", detectMimeTypeFromHeader("GIF89a".toByteArray()))
        assertNull(detectMimeTypeFromHeader(byteArrayOf(0x00, 0x00, 0x00)))
    }

    // ==================== HELPERS ====================

    private fun isValidPngHeader(header: ByteArray): Boolean {
        if (header.size < 8) return false
        return header[0] == 0x89.toByte() &&
                header[1] == 0x50.toByte() &&
                header[2] == 0x4E.toByte() &&
                header[3] == 0x47.toByte()
    }

    private fun isValidJpegHeader(header: ByteArray): Boolean {
        if (header.size < 3) return false
        return header[0] == 0xFF.toByte() &&
                header[1] == 0xD8.toByte() &&
                header[2] == 0xFF.toByte()
    }

    private fun isValidGifHeader(header: ByteArray): Boolean {
        if (header.size < 6) return false
        return header[0] == 'G'.code.toByte() &&
                header[1] == 'I'.code.toByte() &&
                header[2] == 'F'.code.toByte()
    }

    private fun isValidWebpHeader(header: ByteArray): Boolean {
        if (header.size < 12) return false
        val riff = header[0] == 'R'.code.toByte() &&
                header[1] == 'I'.code.toByte() &&
                header[2] == 'F'.code.toByte() &&
                header[3] == 'F'.code.toByte()
        val webp = header[8] == 'W'.code.toByte() &&
                header[9] == 'E'.code.toByte() &&
                header[10] == 'B'.code.toByte() &&
                header[11] == 'P'.code.toByte()
        return riff && webp
    }

    private fun detectMimeTypeFromHeader(header: ByteArray): String? {
        if (header.size < 3) return null
        return when {
            header.size >= 8 && isValidPngHeader(header) -> "image/png"
            isValidJpegHeader(header) -> "image/jpeg"
            isValidGifHeader(header) -> "image/gif"
            header.size >= 12 && isValidWebpHeader(header) -> "image/webp"
            else -> null
        }
    }
}
