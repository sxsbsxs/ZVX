package com.zvx.player

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

/**
 * Keeps one ExoPlayer instance alive while the UI screen is detached.
 * PlaybackService exposes this same player through a MediaSession.
 */
object PlaybackPlayerHolder {
    @Volatile
    private var instance: ExoPlayer? = null

    @Synchronized
    fun get(context: Context): ExoPlayer {
        return instance ?: ExoPlayer.Builder(context.applicationContext).build().also {
            instance = it
        }
    }

    @Synchronized
    fun release() {
        instance?.release()
        instance = null
    }
}
