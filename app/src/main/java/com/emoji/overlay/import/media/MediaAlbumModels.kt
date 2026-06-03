package com.emoji.overlay.import.media

import android.net.Uri

data class MediaAlbum(
    val bucketId: String,
    val name: String,
    val coverUri: Uri?,
    val count: Int
)

data class MediaPhoto(
    val id: Long,
    val uri: Uri,
    val displayName: String
)

internal data class AlbumAccumulator(
    val bucketId: String,
    val name: String,
    var count: Int = 0,
    var coverId: Long = 0L,
    var latestDate: Long = 0L
)
