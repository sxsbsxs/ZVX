package com.zvx.player.data

import android.content.Context

data class LibraryPreferences(
    val filterName: String = "ALL",
    val sortName: String = "NEWEST"
)

object LibraryPreferencesStore {
    private const val PREFS_NAME = "zvx_library_preferences"
    private const val KEY_FILTER = "filter_name"
    private const val KEY_SORT = "sort_name"

    fun load(context: Context): LibraryPreferences {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return LibraryPreferences(
            filterName = preferences.getString(KEY_FILTER, "ALL") ?: "ALL",
            sortName = preferences.getString(KEY_SORT, "NEWEST") ?: "NEWEST"
        )
    }

    fun save(context: Context, filterName: String, sortName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FILTER, filterName.take(32))
            .putString(KEY_SORT, sortName.take(32))
            .apply()
    }
}
