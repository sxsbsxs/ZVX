package com.zvx.player.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class SavedPlaybackQueue(
    val entries: List<MediaEntry> = emptyList(),
    val currentUri: Uri? = null
)

object PlaybackQueueStore {
    private const val PREFS_NAME = "zvx_playback_queue"
    private const val KEY_ENTRIES = "entries"
    private const val KEY_CURRENT_URI = "current_uri"
    private const val MAX_ITEMS = 100

    fun load(context: Context): SavedPlaybackQueue {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = preferences.getString(KEY_ENTRIES, null) ?: return SavedPlaybackQueue()
        val entries = runCatching {
            val json = JSONArray(raw)
            (0 until json.length()).mapNotNull { index ->
                decodeEntry(json.optJSONObject(index))
            }.take(MAX_ITEMS)
        }.getOrDefault(emptyList())
        val currentUri = preferences.getString(KEY_CURRENT_URI, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { Uri.parse(it) }.getOrNull() }
        return SavedPlaybackQueue(
            entries = entries,
            currentUri = currentUri?.takeIf { uri -> entries.any { it.uri == uri } }
                ?: entries.firstOrNull()?.uri
        )
    }

    fun save(context: Context, entries: List<MediaEntry>, currentUri: Uri?) {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (entries.isEmpty()) {
            preferences.edit().remove(KEY_ENTRIES).remove(KEY_CURRENT_URI).apply()
            return
        }
        val safeEntries = entries.distinctBy { it.uri.toString() }.take(MAX_ITEMS)
        val json = JSONArray()
        safeEntries.forEach { entry -> json.put(encodeEntry(entry)) }
        val safeCurrent = currentUri?.takeIf { uri -> safeEntries.any { it.uri == uri } }
            ?: safeEntries.first().uri
        preferences.edit()
            .putString(KEY_ENTRIES, json.toString())
            .putString(KEY_CURRENT_URI, safeCurrent.toString())
            .apply()
    }

    private fun encodeEntry(entry: MediaEntry): JSONObject {
        return JSONObject().apply {
            put("id", entry.id)
            put("uri", entry.uri.toString())
            put("title", entry.title)
            put("display_name", entry.displayName)
            put("mime_type", entry.mimeType)
            put("duration_ms", entry.durationMs)
            put("size_bytes", entry.sizeBytes)
            put("date_added", entry.dateAddedSeconds)
            put("width", entry.width)
            put("height", entry.height)
            put("artist", entry.artist)
            put("album", entry.album)
            put("folder", entry.folderName)
            put("kind", entry.kind.name)
        }
    }

    private fun decodeEntry(json: JSONObject?): MediaEntry? {
        if (json == null) return null
        return runCatching {
            val uri = Uri.parse(json.getString("uri"))
            val kind = MediaKind.valueOf(json.optString("kind", MediaKind.VIDEO.name))
            MediaEntry(
                id = json.optLong("id", 0L),
                uri = uri,
                title = json.optString("title", "Saved media"),
                displayName = json.optString("display_name", uri.lastPathSegment ?: "Saved media"),
                mimeType = json.optString("mime_type").takeIf { it.isNotBlank() && it != "null" },
                durationMs = json.optLong("duration_ms", 0L).coerceAtLeast(0L),
                sizeBytes = json.optLong("size_bytes", 0L).coerceAtLeast(0L),
                dateAddedSeconds = json.optLong("date_added", 0L).coerceAtLeast(0L),
                width = json.optInt("width", -1).takeIf { it > 0 },
                height = json.optInt("height", -1).takeIf { it > 0 },
                artist = json.optString("artist").takeIf { it.isNotBlank() && it != "null" },
                album = json.optString("album").takeIf { it.isNotBlank() && it != "null" },
                folderName = json.optString("folder", "Saved queue"),
                kind = kind
            )
        }.getOrNull()
    }
}
