package com.emoji.overlay.import.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaAlbumGroupingTest {

    @Test
    fun `accumulators merge into sorted albums`() {
        val buckets = linkedMapOf(
            "1" to AlbumAccumulator(bucketId = "1", name = "微信", count = 94, coverId = 10L, latestDate = 100L),
            "2" to AlbumAccumulator(bucketId = "2", name = "QQ", count = 13, coverId = 20L, latestDate = 200L),
            "3" to AlbumAccumulator(bucketId = "3", name = "DCIM", count = 4, coverId = 30L, latestDate = 50L)
        )

        val albums = buckets.values
            .map { acc ->
                MediaAlbum(
                    bucketId = acc.bucketId,
                    name = acc.name,
                    coverUri = null,
                    count = acc.count
                )
            }
            .sortedBy { it.name.lowercase() }

        assertEquals(listOf("DCIM", "QQ", "微信"), albums.map { it.name })
        assertEquals(94, albums.find { it.name == "微信" }?.count)
    }

    @Test
    fun `accumulator picks latest cover id`() {
        val acc = AlbumAccumulator(bucketId = "1", name = "Test")
        acc.count = 3
        acc.latestDate = 10
        acc.coverId = 1L

        val newerDate = 20L
        if (newerDate >= acc.latestDate) {
            acc.latestDate = newerDate
            acc.coverId = 99L
        }

        assertEquals(99L, acc.coverId)
        assertEquals(3, acc.count)
    }

    @Test
    fun `all photos album prepends virtual bucket with total count`() {
        val bucketAlbums = listOf(
            MediaAlbum(bucketId = "1", name = "微信", coverUri = null, count = 94),
            MediaAlbum(bucketId = "2", name = "QQ", coverUri = null, count = 13),
            MediaAlbum(bucketId = "3", name = "DCIM", coverUri = null, count = 4)
        )
        val totalCount = bucketAlbums.sumOf { it.count }
        val allPhotos = MediaAlbum(
            bucketId = MediaAlbumConstants.ALL_PHOTOS_BUCKET_ID,
            name = MediaAlbumConstants.ALL_PHOTOS_NAME,
            coverUri = null,
            count = totalCount
        )
        val albums = listOf(allPhotos) + bucketAlbums

        assertEquals(MediaAlbumConstants.ALL_PHOTOS_NAME, albums.first().name)
        assertEquals(111, albums.first().count)
        assertEquals(4, albums.size)
    }
}
