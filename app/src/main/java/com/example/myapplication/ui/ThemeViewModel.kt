package com.example.myapplication.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.CompanionPreferences
import com.example.myapplication.data.Preferences
import com.example.myapplication.data.ReadingListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThemeViewModel(
    private val repository: ReadingListRepository,
    private val sharedPrefs: SharedPreferences
) : ViewModel() {

    private fun getThemePreferenceWithMigration(prefs: SharedPreferences): String {
        if (prefs.contains("DARK_MODE")) {
            val oldDark = prefs.getBoolean("DARK_MODE", false)
            val migratedTheme = if (oldDark) "OLED" else "LIGHT"
            prefs.edit().remove("DARK_MODE").putString("THEME_PREFERENCE", migratedTheme).apply()
            return migratedTheme
        }
        return prefs.getString("THEME_PREFERENCE", "SYSTEM") ?: "SYSTEM"
    }

    private fun getTimeFormatPreferenceWithMigration(prefs: SharedPreferences): String {
        if (prefs.contains("USE_24_HOUR_TIME")) {
            val old24Hour = prefs.getBoolean("USE_24_HOUR_TIME", false)
            val migratedFormat = if (old24Hour) "24_HOUR" else "12_HOUR"
            prefs.edit().remove("USE_24_HOUR_TIME").putString("TIME_FORMAT_PREFERENCE", migratedFormat).apply()
            return migratedFormat
        }
        return prefs.getString("TIME_FORMAT_PREFERENCE", "SYSTEM") ?: "SYSTEM"
    }

    private val _themePreference = MutableStateFlow(getThemePreferenceWithMigration(sharedPrefs))
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    private val _timeFormatPreference = MutableStateFlow(getTimeFormatPreferenceWithMigration(sharedPrefs))
    val timeFormatPreference: StateFlow<String> = _timeFormatPreference.asStateFlow()

    fun setThemePreference(theme: String) {
        sharedPrefs.edit().putString("THEME_PREFERENCE", theme).apply()
        _themePreference.value = theme
        
        viewModelScope.launch {
            if (!repository.authToken.isNullOrEmpty()) {
                val currentMeResult = repository.getMe()
                if (currentMeResult.isSuccess) {
                    val me = currentMeResult.getOrNull()
                    val newPrefs = me?.preferences?.copy(
                        companion = com.example.myapplication.data.CompanionPreferences(theme = theme)
                    )
                    if (newPrefs != null) {
                        repository.updatePreferences(newPrefs)
                    }
                }
            }
        }
    }

    fun setTimeFormatPreference(format: String) {
        sharedPrefs.edit().putString("TIME_FORMAT_PREFERENCE", format).apply()
        _timeFormatPreference.value = format
        
        viewModelScope.launch {
            if (!repository.authToken.isNullOrEmpty()) {
                val currentMeResult = repository.getMe()
                if (currentMeResult.isSuccess) {
                    val me = currentMeResult.getOrNull()
                    val newPrefs = me?.preferences?.copy(
                        general = com.example.myapplication.data.GeneralPreferences(timeFormat = format)
                    )
                    if (newPrefs != null) {
                        repository.updatePreferences(newPrefs)
                    }
                }
            }
        }
    }

    fun reloadPreferences() {
        _themePreference.value = getThemePreferenceWithMigration(sharedPrefs)
        _timeFormatPreference.value = getTimeFormatPreferenceWithMigration(sharedPrefs)
    }
}
