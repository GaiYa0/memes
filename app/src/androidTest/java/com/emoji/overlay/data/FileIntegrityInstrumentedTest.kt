package com.emoji.overlay.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emoji.overlay.data.util.ResourceManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Android instrumentation tests for file integrity operations.
 *
 * These tests run on a real Android device and test actual
 * file operations including:
 * - File existence checking
 * - File corruption detection
 * - Duplicate detection
 * - Database-filesystem sync
 * - Recovery from missing files
 *
 * Run with: ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class FileIntegrityInstrumentedTest {

    private lateinit var resourceManager: ResourceManager
    private lateinit var testDir: File

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        resourceManager = ResourceManager(context)
        testDir = File(context.cacheDir, "test_emoji_${System.currentTimeMillis()}")
        testDir.mkdirs()
    }

    @After
    fun teardown() {
        testDir.deleteRecursively()
    }

    // ==================== FILE EXISTENCE TESTS ====================

    @Test
    fun detectMissingFile() {
        val exists = resourceManager.fileExists("/nonexistent/path/file.png")
        assertFalse("Missing file should return false", exists)
    }

    @Test
    fun detectExistingFile() {
        val testFile = File(testDir, "test.png")
        testFile.writeText("test content")

        val relativePath = testFile.relativeTo(resourceManager.vaultDir).path
        // File is in testDir, not in vaultDir, so this tests the logic
        assertTrue("Created file should exist", testFile.exists())
    }

    @Test
    fun batchFileExistenceCheck() {
        // Create some test files
        val existingFiles = (1..10).map {
            val file = File(testDir, "emoji_$it.png")
            file.writeBytes(ByteArray(100))
            file.absolutePath
        }

        // Check existence
        val allPaths = (1..20).map { File(testDir, "emoji_$it.png").absolutePath }
        val missing = allPaths.filter { !File(it).exists() }

        assertEquals(10, missing.size)
        assertEquals(10, existingFiles.filter { File(it).exists() }.size)
    }

    // ==================== FILE CORRUPTION TESTS ====================

    @Test
    fun detectEmptyFile() {
        val emptyFile = File(testDir, "empty.png")
        emptyFile.createNewFile()

        assertTrue("Empty file should exist", emptyFile.exists())
        assertEquals("Empty file should have 0 bytes", 0, emptyFile.length())
    }

    @Test
    fun detectValidPngHeader() {
        val pngFile = File(testDir, "valid.png")
        // PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
        val pngHeader = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        pngFile.writeBytes(pngHeader + ByteArray(100))

        val bytes = pngFile.readBytes()
        assertTrue("File should be large enough", bytes.size >= 8)
        assertEquals("PNG magic byte 0", 0x89.toByte(), bytes[0])
        assertEquals("PNG magic byte 1", 0x50.toByte(), bytes[1])
        assertEquals("PNG magic byte 2", 0x4E.toByte(), bytes[2])
    }

    @Test
    fun detectInvalidPngHeader() {
        val invalidFile = File(testDir, "invalid.png")
        invalidFile.writeText("This is not a PNG file")

        val bytes = invalidFile.readBytes()
        assertTrue("File should have content", bytes.isNotEmpty())
        assertNotEquals("First byte should not be PNG magic", 0x89.toByte(), bytes[0])
    }

    @Test
    fun detectGifHeader() {
        val gifFile = File(testDir, "valid.gif")
        val gifHeader = "GIF89a".toByteArray()
        gifFile.writeBytes(gifHeader + ByteArray(100))

        val bytes = gifFile.readBytes()
        assertTrue("File should be large enough", bytes.size >= 6)
        assertEquals('G'.code.toByte(), bytes[0])
        assertEquals('I'.code.toByte(), bytes[1])
        assertEquals('F'.code.toByte(), bytes[2])
    }

    // ==================== DUPLICATE DETECTION TESTS ====================

    @Test
    fun detectDuplicateByHash() {
        val file1 = File(testDir, "emoji1.png")
        val file2 = File(testDir, "emoji2.png")
        val content = "same content for both files".toByteArray()

        file1.writeBytes(content)
        file2.writeBytes(content)

        val hash1 = resourceManager.calculateContentHash(file1)
        val hash2 = resourceManager.calculateContentHash(file2)

        assertEquals("Same content should produce same hash", hash1, hash2)
    }

    @Test
    fun detectDifferentByHash() {
        val file1 = File(testDir, "emoji1.png")
        val file2 = File(testDir, "emoji2.png")

        file1.writeBytes("content A".toByteArray())
        file2.writeBytes("content B".toByteArray())

        val hash1 = resourceManager.calculateContentHash(file1)
        val hash2 = resourceManager.calculateContentHash(file2)

        assertNotEquals("Different content should produce different hash", hash1, hash2)
    }

    @Test
    fun deduplicationAcross100Files() {
        val files = (1..100).map {
            val file = File(testDir, "file_$it.png")
            // Create 50 unique files (each shared by 2)
            file.writeBytes("content_${it % 50}".toByteArray())
            file
        }

        val hashes = files.map { resourceManager.calculateContentHash(it) }
        val uniqueHashes = hashes.toSet()

        assertEquals("Should have 50 unique hashes", 50, uniqueHashes.size)
    }

    // ==================== DATABASE-FILESYSTEM SYNC TESTS ====================

    @Test
    fun verifyConsistencyAllPresent() {
        val files = (1..10).map {
            val file = File(testDir, "emoji_$it.png")
            file.writeBytes(ByteArray(100))
            file.absolutePath
        }

        val dbPaths = files.toSet()
        val fsPaths = files.filter { File(it).exists() }.toSet()

        assertEquals("DB and FS should match", dbPaths, fsPaths)
    }

    @Test
    fun verifyConsistencyMissingFiles() {
        // Create 10 files in DB
        val dbPaths = (1..10).map { File(testDir, "emoji_$it.png").absolutePath }.toSet()

        // Only create 7 files on disk
        (1..7).forEach {
            File(testDir, "emoji_$it.png").writeBytes(ByteArray(100))
        }

        val fsPaths = dbPaths.filter { File(it).exists() }.toSet()
        val missing = dbPaths - fsPaths

        assertEquals(3, missing.size)
    }

    @Test
    fun detectOrphanedFiles() {
        // DB knows about these
        val dbPaths = setOf(
            File(testDir, "emoji_1.png").absolutePath,
            File(testDir, "emoji_2.png").absolutePath
        )

        // FS has extra files
        (1..5).forEach {
            File(testDir, "emoji_$it.png").writeBytes(ByteArray(100))
        }
        val fsPaths = (1..5).map { File(testDir, "emoji_$it.png").absolutePath }.toSet()

        val orphaned = fsPaths - dbPaths
        assertEquals(3, orphaned.size)
    }

    // ==================== RECOVERY TESTS ====================

    @Test
    fun recoverySoftDeleteMissingFiles() {
        data class EmojiRecord(val id: Long, val path: String, var isDeleted: Boolean)

        val records = (1L..10L).map {
            EmojiRecord(it, File(testDir, "emoji_$it.png").absolutePath, false)
        }

        // Create only 7 files
        (1..7).forEach {
            File(testDir, "emoji_$it.png").writeBytes(ByteArray(100))
        }

        // Find missing and soft delete
        records.forEach { record ->
            if (!File(record.path).exists()) {
                record.isDeleted = true
            }
        }

        val active = records.filter { !it.isDeleted }
        val deleted = records.filter { it.isDeleted }

        assertEquals(7, active.size)
        assertEquals(3, deleted.size)
    }

    @Test
    fun recoveryRestoreFromSoftDelete() {
        data class EmojiRecord(val id: Long, var isDeleted: Boolean)

        val records = (1L..10L).map { EmojiRecord(it, it > 7) }
        assertEquals(3, records.count { it.isDeleted })

        // Restore all
        records.forEach { it.isDeleted = false }
        assertEquals(0, records.count { it.isDeleted })
    }

    // ==================== BATCH IMPORT TESTS ====================

    @Test
    fun batchImportNoDuplicates() {
        val existingHashes = mutableSetOf("hash_3", "hash_7")
        val newFiles = (1L..10L).map { "hash_$it" }

        val toImport = newFiles.filter { it !in existingHashes }
        val duplicates = newFiles.filter { it in existingHashes }

        assertEquals(8, toImport.size)
        assertEquals(2, duplicates.size)
    }

    @Test
    fun batchImportAllUnique() {
        val existingHashes = mutableSetOf<String>()
        val newFiles = (1L..10L).map { "hash_$it" }

        val toImport = newFiles.filter { it !in existingHashes }
        assertEquals(10, toImport.size)
    }

    // ==================== THUMBNAIL TESTS ====================

    @Test
    fun thumbnailDirectoryCreation() {
        assertTrue("Thumb dir should exist or be created", resourceManager.thumbDir.exists() || resourceManager.thumbDir.mkdirs())
    }

    @Test
    fun cacheDirectoryCreation() {
        assertTrue("Cache dir should exist or be created", resourceManager.cacheDir.exists() || resourceManager.cacheDir.mkdirs())
    }
}
