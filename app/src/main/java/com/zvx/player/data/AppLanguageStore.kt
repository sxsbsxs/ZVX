package com.zvx.player.data

import android.content.Context
import java.util.Locale

object AppLanguageStore {
    private const val PREFS_NAME = "zvx_app_language"
    private const val KEY_LANGUAGE = "language_code"
    val supportedCodes = listOf("en", "zh", "ja")

    fun load(context: Context): String {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
        if (saved in supportedCodes) return saved.orEmpty()
        return when (Locale.getDefault().language.lowercase(Locale.ROOT)) {
            "zh" -> "zh"
            "ja" -> "ja"
            else -> "en"
        }
    }

    fun save(context: Context, languageCode: String): String {
        val safeCode = languageCode.takeIf { it in supportedCodes } ?: "en"
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, safeCode)
            .apply()
        return safeCode
    }
}
