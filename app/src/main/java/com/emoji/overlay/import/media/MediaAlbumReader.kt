package com.emoji.overlay.import.media

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore

object MediaAlbumConstants {
    const val ALL_PHOTOS_BUCKET_ID = "__ALL_PHOTOS__"
    const val ALL_PHOTOS_NAME = "全部照片"
}

class MediaAlbumReader(private val contentResolver: ContentResolver) {

    fun loadAlbums(): List<MediaAlbum> {
        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Images.Media.MIME_TYPE} LIKE ? OR ${MediaStore.Images.Media.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("image/%", "image/gif")
        val sortOrder = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} ASC"

        val buckets = linkedMapOf<String, AlbumAccumulator>()

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val bucketId = cursor.getString(bucketIdCol) ?: continue
                val bucketName = cursor.getString(bucketNameCol)?.takeIf { it.isNotBlank() } ?: "未知相册"
                val mediaId = cursor.getLong(idCol)
                val dateAdded = cursor.getLong(dateCol)

                val acc = buckets.getOrPut(bucketId) {
                    AlbumAccumulator(bucketId = bucketId, name = bucketName)
                }
                acc.count++
                if (dateAdded >= acc.latestDate) {
                    acc.latestDate = dateAdded
                    acc.coverId = mediaId
                }
            }
        }

        val bucketAlbums = buckets.values
            .map { acc ->
                MediaAlbum(
                    bucketId = acc.bucketId,
                    name = acc.name,
                    coverUri = if (acc.coverId > 0L) {
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            acc.coverId
                        )
                    } else {
                        null
                    },
                    count = acc.count
                )
            }
            .sortedBy { it.name.lowercase() }

        if (bucketAlbums.isEmpty()) return emptyList()

        val totalCount = bucketAlbums.sumOf { it.count }
        val latestAcc = buckets.values.maxByOrNull { it.latestDate }
        val allPhotosCover = latestAcc?.takeIf { it.coverId > 0L }?.let {
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it.coverId)
        }

        val allPhotosAlbum = MediaAlbum(
            bucketId = MediaAlbumConstants.ALL_PHOTOS_BUCKET_ID,
            name = MediaAlbumConstants.ALL_PHOTOS_NAME,
            coverUri = allPhotosCover,
            count = totalCount
        )

        return listOf(allPhotosAlbum) + bucketAlbums
    }

    fun loadPhotos(bucketId: String): List<MediaPhoto> {
        if (bucketId == MediaAlbumConstants.ALL_PHOTOS_BUCKET_ID) {
            return loadAllPhotos()
        }
        return loadPhotosForBucket(bucketId)
    }

    private fun loadAllPhotos(): List<MediaPhoto> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Images.Media.MIME_TYPE} LIKE ? OR ${MediaStore.Images.Media.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("image/%", "image/gif")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        return queryPhotos(projection, selection, selectionArgs, sortOrder)
    }

    private fun loadPhotosForBucket(bucketId: String): List<MediaPhoto> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME
        )
        val selection =
            "${MediaStore.Images.Media.BUCKET_ID} = ? AND (${MediaStore.Images.Media.MIME_TYPE} LIKE ? OR ${MediaStore.Images.Media.MIME_TYPE} = ?)"
        val selectionArgs = arrayOf(bucketId, "image/%", "image/gif")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        return queryPhotos(projection, selection, selectionArgs, sortOrder)
    }

    private fun queryPhotos(
        projection: Array<String>,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String
    ): List<MediaPhoto> {
        val photos = mutableListOf<MediaPhoto>()
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "image_$id"
                photos.add(
                    MediaPhoto(
                        id = id,
                        uri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        ),
                        displayName = name
                    )
                )
            }
        }
        return photos
    }
}
