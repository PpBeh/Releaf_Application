package com.example.releaf.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.releaf.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage {
    ENGLISH, CHINESE
}

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    private val _theme = MutableStateFlow(loadTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _language = MutableStateFlow(loadLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setTheme(theme: AppTheme) {
        _theme.value = theme
        prefs.edit().putString("theme_mode", theme.name).apply()
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        prefs.edit().putString("language_mode", lang.name).apply()
    }

    private fun loadTheme(): AppTheme {
        val name = prefs.getString("theme_mode", AppTheme.SYSTEM.name)
        return try { AppTheme.valueOf(name ?: AppTheme.SYSTEM.name) } catch (e: Exception) { AppTheme.SYSTEM }
    }

    private fun loadLanguage(): AppLanguage {
        val name = prefs.getString("language_mode", AppLanguage.ENGLISH.name)
        return try { AppLanguage.valueOf(name ?: AppLanguage.ENGLISH.name) } catch (e: Exception) { AppLanguage.ENGLISH }
    }
}
