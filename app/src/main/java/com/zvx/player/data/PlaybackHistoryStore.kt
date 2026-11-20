package com.zvx.player.data

import android.content.Context
import org.json.JSONArray

object PlaybackHistoryStore {
    private const val PREFS_NAME = "zvx_playback_history"
    private const val KEY_RECENT = "recent_uris"
    private const val MAX_ITEMS = 50

    fun getRecent(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_RECENT, null) ?: return emptyList()

        return runCatching {
            val json = JSONArray(raw)
            (0 until json.length()).mapNotNull { index ->
                json.optString(index).takeIf { it.isNotBlank() && it != "null" }
            }
        }.getOrDefault(emptyList())
    }

    fun markPlayed(context: Context, uri: String) {
        if (uri.isBlank()) return
        val updated = (listOf(uri) + getRecent(context).filterNot { it == uri }).take(MAX_ITEMS)
        val json = JSONArray()
        updated.forEach(json::put)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_RECENT, json.toString())
            .apply()
    }
}
