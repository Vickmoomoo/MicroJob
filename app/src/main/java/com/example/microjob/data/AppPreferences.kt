package com.example.microjob.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences private constructor(context: Context) {

    companion object {
        const val THEME_SYSTEM = "System"
        const val THEME_LIGHT = "Light"
        const val THEME_DARK = "Dark"
        const val LANGUAGE_ENGLISH = "English"
        const val LANGUAGE_CHINESE = "Chinese"
        const val LANGUAGE_MALAY = "Malay"

        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(prefs.getString("theme", THEME_SYSTEM) ?: THEME_SYSTEM)
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString("language", LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH)
    val language: StateFlow<String> = _language.asStateFlow()

    fun setTheme(value: String) {
        prefs.edit().putString("theme", value).apply()
        _theme.value = value
    }

    fun setLanguage(value: String) {
        prefs.edit().putString("language", value).apply()
        _language.value = value
    }
}
