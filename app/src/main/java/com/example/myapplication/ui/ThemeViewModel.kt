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

    private val _isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("DARK_MODE", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _use24HourTime = MutableStateFlow(sharedPrefs.getBoolean("USE_24_HOUR_TIME", false))
    val use24HourTime: StateFlow<Boolean> = _use24HourTime.asStateFlow()

    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        sharedPrefs.edit().putBoolean("DARK_MODE", isDark).apply()
        
        // Sync with backend if logged in
        if (repository.authToken != null) {
            viewModelScope.launch {
                repository.updatePreferences(
                    Preferences(
                        companion = CompanionPreferences(darkMode = isDark),
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
                        companion = CompanionPreferences(darkMode = _isDarkMode.value),
                        general = com.example.myapplication.data.GeneralPreferences(use24HourTime = use24Hour)
                    )
                )
            }
        }
    }
}
