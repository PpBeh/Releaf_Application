package com.example.releaf.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.releaf.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    
    private val _theme = MutableStateFlow(loadTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    fun setTheme(theme: AppTheme) {
        _theme.value = theme
        prefs.edit().putString("theme_mode", theme.name).apply()
    }

    private fun loadTheme(): AppTheme {
        val name = prefs.getString("theme_mode", AppTheme.SYSTEM.name)
        return try { AppTheme.valueOf(name ?: AppTheme.SYSTEM.name) } catch (e: Exception) { AppTheme.SYSTEM }
    }
}
