package com.zvx.player.data

import android.content.Context
import android.net.Uri

object PlaybackProgressStore {
    private const val PREFS_NAME = "zvx_playback_progress"
    private const val PREFIX = "position:"

    fun getPosition(context: Context, uri: String): Long {
        if (uri.isBlank()) return 0L
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(key(uri), 0L)
            .coerceAtLeast(0L)
    }

    fun save(context: Context, uri: String, positionMs: Long, durationMs: Long) {
        if (uri.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val safePosition = positionMs.coerceAtLeast(0L)
        val finished = durationMs > 0L && safePosition >= (durationMs - 10_000L).coerceAtLeast(0L)
        if (safePosition < 5_000L || durationMs <= 0L || finished) {
            prefs.edit().remove(key(uri)).apply()
        } else {
            prefs.edit().putLong(key(uri), safePosition).apply()
        }
    }

    private fun key(uri: String): String = PREFIX + Uri.encode(uri)
}
