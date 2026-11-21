package com.zvx.player.data

import android.content.Context
import org.json.JSONArray

object MediaFavoritesStore {
    private const val PREFS_NAME = "zvx_media_favorites"
    private const val KEY_FAVORITES = "favorite_uris"

    fun getFavorites(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FAVORITES, null) ?: return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            (0 until json.length()).mapNotNull { index ->
                json.optString(index).takeIf { it.isNotBlank() && it != "null" }
            }.distinct()
        }.getOrDefault(emptyList())
    }

    fun toggle(context: Context, uri: String): List<String> {
        if (uri.isBlank()) return getFavorites(context)
        val current = getFavorites(context)
        val updated = if (uri in current) current.filterNot { it == uri } else current + uri
        val json = JSONArray()
        updated.forEach(json::put)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FAVORITES, json.toString())
            .apply()
        return updated
    }
}
