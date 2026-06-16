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
        return prefs.getString("THEME_PREFERENCE", "LIGHT") ?: "LIGHT"
    }

    private val _themePreference = MutableStateFlow(getThemePreferenceWithMigration(sharedPrefs))
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    private val _use24HourTime = MutableStateFlow(sharedPrefs.getBoolean("USE_24_HOUR_TIME", false))
    val use24HourTime: StateFlow<Boolean> = _use24HourTime.asStateFlow()

    fun setThemePreference(theme: String) {
        _themePreference.value = theme
        sharedPrefs.edit().putString("THEME_PREFERENCE", theme).apply()
        
        // Sync with backend if logged in
        if (repository.authToken != null) {
            viewModelScope.launch {
                repository.updatePreferences(
                    Preferences(
                        companion = CompanionPreferences(theme = theme),
                        general = com.example.myapplication.data.GeneralPreferences(use24HourTime = _use24HourTime.value)
                    )
                )
            }
        }
    }

    fun setUse24HourTime(use24Hour: Boolean) {
        _use24HourTime.value = use24Hour
        sharedPrefs.edit().putBoolean("USE_24_HOUR_TIME", use24Hour).apply()
        
        // Sync with backend if logged in
        if (repository.authToken != null) {
            viewModelScope.launch {
                repository.updatePreferences(
                    Preferences(
                        companion = CompanionPreferences(theme = _themePreference.value),
                        general = com.example.myapplication.data.GeneralPreferences(use24HourTime = use24Hour)
                    )
                )
            }
        }
    }
    fun reloadPreferences() {
        _themePreference.value = getThemePreferenceWithMigration(sharedPrefs)
        _use24HourTime.value = sharedPrefs.getBoolean("USE_24_HOUR_TIME", false)
    }
}
