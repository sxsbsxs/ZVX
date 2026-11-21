package com.zvx.player.data

import android.content.Context

data class PlaybackPreferences(
    val speedIndex: Int = 1,
    val aspectMode: Int = 0,
    val subtitleSizeSp: Int = 18,
    val subtitleBackgroundEnabled: Boolean = true,
    val subtitleDelayMs: Int = 0
)

object PlaybackSettingsStore {
    private const val PREFS_NAME = "zvx_playback_settings"
    private const val KEY_SPEED_INDEX = "speed_index"
    private const val KEY_ASPECT_MODE = "aspect_mode"
    private const val KEY_SUBTITLE_SIZE = "subtitle_size_sp"
    private const val KEY_SUBTITLE_BACKGROUND = "subtitle_background"
    private const val KEY_SUBTITLE_DELAY = "subtitle_delay_ms"

    fun load(context: Context): PlaybackPreferences {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return PlaybackPreferences(
            speedIndex = preferences.getInt(KEY_SPEED_INDEX, 1).coerceIn(0, 4),
            aspectMode = preferences.getInt(KEY_ASPECT_MODE, 0).coerceIn(0, 2),
            subtitleSizeSp = preferences.getInt(KEY_SUBTITLE_SIZE, 18).coerceIn(14, 32),
            subtitleBackgroundEnabled = preferences.getBoolean(KEY_SUBTITLE_BACKGROUND, true),
            subtitleDelayMs = preferences.getInt(KEY_SUBTITLE_DELAY, 0).coerceIn(-10_000, 10_000)
        )
    }

    fun save(context: Context, preferences: PlaybackPreferences) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SPEED_INDEX, preferences.speedIndex.coerceIn(0, 4))
            .putInt(KEY_ASPECT_MODE, preferences.aspectMode.coerceIn(0, 2))
            .putInt(KEY_SUBTITLE_SIZE, preferences.subtitleSizeSp.coerceIn(14, 32))
            .putBoolean(KEY_SUBTITLE_BACKGROUND, preferences.subtitleBackgroundEnabled)
            .putInt(KEY_SUBTITLE_DELAY, preferences.subtitleDelayMs.coerceIn(-10_000, 10_000))
            .apply()
    }
}
