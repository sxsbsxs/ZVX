package com.zvx.player.data

import android.net.Uri

enum class MediaKind {
    VIDEO, AUDIO
}

data class MediaEntry(
    val id: Long,
    val uri: Uri,
    val title: String,
    val displayName: String,
    val mimeType: String?,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSeconds: Long,
    val width: Int? = null,
    val height: Int? = null,
    val artist: String? = null,
    val album: String? = null,
    val kind: MediaKind
)
