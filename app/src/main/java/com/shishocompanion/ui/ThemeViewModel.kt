package com.shishocompanion.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shishocompanion.data.CompanionPreferences
import com.shishocompanion.data.Preferences
import com.shishocompanion.data.ReadingListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
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

    private fun getDateFormatPreferenceWithMigration(prefs: SharedPreferences): String {
        return prefs.getString("DATE_FORMAT_PREFERENCE", "SYSTEM") ?: "SYSTEM"
    }

    private val _themePreference = MutableStateFlow(getThemePreferenceWithMigration(sharedPrefs))
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    private val _timeFormatPreference = MutableStateFlow(getTimeFormatPreferenceWithMigration(sharedPrefs))
    val timeFormatPreference: StateFlow<String> = _timeFormatPreference.asStateFlow()

    private val _dateFormatPreference = MutableStateFlow(getDateFormatPreferenceWithMigration(sharedPrefs))
    val dateFormatPreference: StateFlow<String> = _dateFormatPreference.asStateFlow()

    private val _languagePreference = MutableStateFlow(sharedPrefs.getString("LANGUAGE_PREFERENCE", "SYSTEM") ?: "SYSTEM")
    val languagePreference: StateFlow<String> = _languagePreference.asStateFlow()

    private val _sendRemindersToDiscordPreference = MutableStateFlow(sharedPrefs.getBoolean("SEND_REMINDERS_TO_DISCORD", false))
    val sendRemindersToDiscordPreference: StateFlow<Boolean> = _sendRemindersToDiscordPreference.asStateFlow()

    private val _splitScreenPreference = MutableStateFlow(sharedPrefs.getBoolean("SPLIT_SCREEN_ENABLED", true))
    val splitScreenPreference: StateFlow<Boolean> = _splitScreenPreference.asStateFlow()

    private val preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "THEME_PREFERENCE", "DARK_MODE" -> _themePreference.value = getThemePreferenceWithMigration(sharedPrefs)
            "TIME_FORMAT_PREFERENCE", "USE_24_HOUR_TIME" -> _timeFormatPreference.value = getTimeFormatPreferenceWithMigration(sharedPrefs)
            "DATE_FORMAT_PREFERENCE" -> _dateFormatPreference.value = sharedPrefs.getString("DATE_FORMAT_PREFERENCE", "SYSTEM") ?: "SYSTEM"
            "LANGUAGE_PREFERENCE" -> _languagePreference.value = sharedPrefs.getString("LANGUAGE_PREFERENCE", "SYSTEM") ?: "SYSTEM"
            "SEND_REMINDERS_TO_DISCORD" -> _sendRemindersToDiscordPreference.value = sharedPrefs.getBoolean("SEND_REMINDERS_TO_DISCORD", false)
            "SPLIT_SCREEN_ENABLED" -> _splitScreenPreference.value = sharedPrefs.getBoolean("SPLIT_SCREEN_ENABLED", true)
        }
    }

    init {
        sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun setThemePreference(theme: String) {
        sharedPrefs.edit().putString("THEME_PREFERENCE", theme).apply()
        _themePreference.value = theme
        
        viewModelScope.launch {
            if (!repository.authToken.isNullOrEmpty()) {
                val currentMeResult = repository.getMe()
                if (currentMeResult.isSuccess) {
                    val me = currentMeResult.getOrNull()
                    val currentCompanion = me?.preferences?.companion ?: com.shishocompanion.data.CompanionPreferences()
                    val newPrefs = me?.preferences?.copy(
                        companion = currentCompanion.copy(theme = theme)
                    ) ?: com.shishocompanion.data.Preferences(
                        companion = currentCompanion.copy(theme = theme)
                    )
                    repository.updatePreferences(newPrefs)
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
                    val currentGeneral = me?.preferences?.general ?: com.shishocompanion.data.GeneralPreferences()
                    val newPrefs = me?.preferences?.copy(
                        general = currentGeneral.copy(timeFormat = format)
                    ) ?: com.shishocompanion.data.Preferences(
                        general = com.shishocompanion.data.GeneralPreferences(timeFormat = format)
                    )
                    repository.updatePreferences(newPrefs)
                }
            }
        }
    }

    fun setDateFormatPreference(format: String) {
        sharedPrefs.edit().putString("DATE_FORMAT_PREFERENCE", format).apply()
        _dateFormatPreference.value = format
        
        viewModelScope.launch {
            if (!repository.authToken.isNullOrEmpty()) {
                val currentMeResult = repository.getMe()
                if (currentMeResult.isSuccess) {
                    val me = currentMeResult.getOrNull()
                    val currentGeneral = me?.preferences?.general ?: com.shishocompanion.data.GeneralPreferences()
                    val newPrefs = me?.preferences?.copy(
                        general = currentGeneral.copy(dateFormat = format)
                    ) ?: com.shishocompanion.data.Preferences(
                        general = com.shishocompanion.data.GeneralPreferences(dateFormat = format)
                    )
                    repository.updatePreferences(newPrefs)
                }
            }
        }
    }

    fun setLanguagePreference(lang: String) {
        sharedPrefs.edit().putString("LANGUAGE_PREFERENCE", lang).apply()
        _languagePreference.value = lang
        
        viewModelScope.launch {
            if (!repository.authToken.isNullOrEmpty()) {
                val currentMeResult = repository.getMe()
                if (currentMeResult.isSuccess) {
                    val me = currentMeResult.getOrNull()
                    val currentGeneral = me?.preferences?.general ?: com.shishocompanion.data.GeneralPreferences()
                    val newPrefs = me?.preferences?.copy(
                        general = currentGeneral.copy(language = lang)
                    ) ?: com.shishocompanion.data.Preferences(
                        general = com.shishocompanion.data.GeneralPreferences(language = lang)
                    )
                    repository.updatePreferences(newPrefs)
                }
            }
        }
    }

    fun setSplitScreenPreference(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("SPLIT_SCREEN_ENABLED", enabled).apply()
        _splitScreenPreference.value = enabled
        
        viewModelScope.launch {
            if (!repository.authToken.isNullOrEmpty()) {
                val currentMeResult = repository.getMe()
                if (currentMeResult.isSuccess) {
                    val me = currentMeResult.getOrNull()
                    val currentCompanion = me?.preferences?.companion ?: com.shishocompanion.data.CompanionPreferences()
                    val newPrefs = me?.preferences?.copy(
                        companion = currentCompanion.copy(splitScreen = enabled)
                    ) ?: com.shishocompanion.data.Preferences(
                        companion = currentCompanion.copy(splitScreen = enabled)
                    )
                    repository.updatePreferences(newPrefs)
                }
            }
        }
    }

    fun setSendRemindersToDiscordPreference(send: Boolean) {
        sharedPrefs.edit().putBoolean("SEND_REMINDERS_TO_DISCORD", send).apply()
        _sendRemindersToDiscordPreference.value = send
        
        viewModelScope.launch {
            if (!repository.authToken.isNullOrEmpty()) {
                val currentMeResult = repository.getMe()
                if (currentMeResult.isSuccess) {
                    val me = currentMeResult.getOrNull()
                    val currentGeneral = me?.preferences?.general ?: com.shishocompanion.data.GeneralPreferences()
                    val newPrefs = me?.preferences?.copy(
                        general = currentGeneral.copy(sendRemindersToDiscord = send)
                    ) ?: com.shishocompanion.data.Preferences(
                        general = com.shishocompanion.data.GeneralPreferences(sendRemindersToDiscord = send)
                    )
                    repository.updatePreferences(newPrefs)
                }
            }
        }
    }

    fun reloadPreferences() {
        _themePreference.value = getThemePreferenceWithMigration(sharedPrefs)
        _timeFormatPreference.value = getTimeFormatPreferenceWithMigration(sharedPrefs)
        _dateFormatPreference.value = sharedPrefs.getString("DATE_FORMAT_PREFERENCE", "SYSTEM") ?: "SYSTEM"
        _sendRemindersToDiscordPreference.value = sharedPrefs.getBoolean("SEND_REMINDERS_TO_DISCORD", false)
        _languagePreference.value = sharedPrefs.getString("LANGUAGE_PREFERENCE", "SYSTEM") ?: "SYSTEM"
        _splitScreenPreference.value = sharedPrefs.getBoolean("SPLIT_SCREEN_ENABLED", true)
    }
}
