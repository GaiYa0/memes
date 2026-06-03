package com.emoji.overlay.send

import android.net.Uri

data class SharePayload(
    val uri: Uri,
    val mimeType: String,
    val title: String
)
