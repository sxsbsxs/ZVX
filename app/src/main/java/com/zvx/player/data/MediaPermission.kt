package com.zvx.player.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class MediaAccess(
    val hasVideo: Boolean,
    val hasAudio: Boolean
) {
    val hasAny: Boolean get() = hasVideo || hasAudio
}

object MediaPermission {
    fun current(context: Context): MediaAccess {
        fun granted(permission: String): Boolean =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

        return when {
            Build.VERSION.SDK_INT >= 34 -> MediaAccess(
                hasVideo = granted(Manifest.permission.READ_MEDIA_VIDEO) ||
                    granted("android.permission.READ_MEDIA_VISUAL_USER_SELECTED"),
                hasAudio = granted(Manifest.permission.READ_MEDIA_AUDIO)
            )
            Build.VERSION.SDK_INT >= 33 -> MediaAccess(
                hasVideo = granted(Manifest.permission.READ_MEDIA_VIDEO),
                hasAudio = granted(Manifest.permission.READ_MEDIA_AUDIO)
            )
            else -> {
                val storage = granted(Manifest.permission.READ_EXTERNAL_STORAGE)
                MediaAccess(hasVideo = storage, hasAudio = storage)
            }
        }
    }

    fun permissionsToRequest(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
