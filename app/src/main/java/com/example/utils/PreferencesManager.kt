package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object PreferencesManager {
    private const val PREFS_NAME = "pulse_app_prefs"
    private const val KEY_THEME = "app_theme"
    private const val KEY_LANGUAGE = "app_language"

    const val THEME_SYSTEM = "SYSTEM"
    const val THEME_LIGHT = "LIGHT"
    const val THEME_DARK = "DARK"

    const val LANG_SYSTEM = "SYSTEM"
    const val LANG_ARABIC = "ARABIC"
    const val LANG_ENGLISH = "ENGLISH"

    private lateinit var sharedPreferences: SharedPreferences

    private val _themeFlow = MutableStateFlow(THEME_SYSTEM)
    val themeFlow: StateFlow<String> = _themeFlow.asStateFlow()

    private val _languageFlow = MutableStateFlow(LANG_SYSTEM)
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _themeFlow.value = sharedPreferences.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        _languageFlow.value = sharedPreferences.getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
    }

    fun isInitialized(): Boolean = ::sharedPreferences.isInitialized

    fun setTheme(theme: String) {
        _themeFlow.value = theme
        if (::sharedPreferences.isInitialized) {
            sharedPreferences.edit().putString(KEY_THEME, theme).apply()
        }
    }

    fun setLanguage(language: String) {
        _languageFlow.value = language
        if (::sharedPreferences.isInitialized) {
            sharedPreferences.edit().putString(KEY_LANGUAGE, language).apply()
        }
    }

    fun getLocale(): Locale {
        return when (_languageFlow.value) {
            LANG_ARABIC -> Locale("ar")
            LANG_ENGLISH -> Locale("en")
            else -> Locale.getDefault()
        }
    }

    fun getLayoutDirection(): LayoutDirection {
        val locale = getLocale()
        return if (locale.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    }

    fun applyLocaleContext(baseContext: Context): Context {
        val targetLocale = getLocale()
        Locale.setDefault(targetLocale)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(targetLocale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(targetLocale))
        }
        return baseContext.createConfigurationContext(config)
    }
}
