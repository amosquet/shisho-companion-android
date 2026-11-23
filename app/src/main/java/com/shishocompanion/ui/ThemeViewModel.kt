package com.shishocompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shishocompanion.data.AuthRepository
import com.shishocompanion.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    val themePreference: StateFlow<String> = userPrefs.themePreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val dynamicColorPreference: StateFlow<Boolean> = userPrefs.dynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val timeFormatPreference: StateFlow<String> = userPrefs.timeFormatPreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val dateFormatPreference: StateFlow<String> = userPrefs.dateFormatPreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val languagePreference: StateFlow<String> = userPrefs.languagePreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val sendRemindersToDiscordPreference: StateFlow<Boolean> = userPrefs.sendRemindersToDiscord
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val splitScreenPreference: StateFlow<Boolean> = userPrefs.splitScreenEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val swipeActionPreference: StateFlow<String> = userPrefs.swipeAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DELETE")

    private suspend fun updateRemotePreferences(
        transform: (com.shishocompanion.data.Preferences) -> com.shishocompanion.data.Preferences
    ) {
        if (authRepository.authToken.isNullOrEmpty()) return
        val me = authRepository.getMe().getOrNull() ?: return
        val basePrefs = me.preferences ?: com.shishocompanion.data.Preferences()
        authRepository.updatePreferences(transform(basePrefs))
    }

    fun setThemePreference(theme: String) {
        viewModelScope.launch {
            userPrefs.setThemePreference(theme)
            updateRemotePreferences { prefs ->
                prefs.copy(companion = prefs.companion.copy(theme = theme))
            }
        }
    }

    fun setTimeFormatPreference(format: String) {
        viewModelScope.launch {
            userPrefs.setTimeFormatPreference(format)
            updateRemotePreferences { prefs ->
                prefs.copy(general = prefs.general.copy(timeFormat = format))
            }
        }
    }

    fun setDateFormatPreference(format: String) {
        viewModelScope.launch {
            userPrefs.setDateFormatPreference(format)
            updateRemotePreferences { prefs ->
                prefs.copy(general = prefs.general.copy(dateFormat = format))
            }
        }
    }

    fun setLanguagePreference(lang: String) {
        viewModelScope.launch {
            userPrefs.setLanguagePreference(lang)
            updateRemotePreferences { prefs ->
                prefs.copy(general = prefs.general.copy(language = lang))
            }
        }
    }

    fun setSplitScreenPreference(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setSplitScreenEnabled(enabled)
            updateRemotePreferences { prefs ->
                prefs.copy(companion = prefs.companion.copy(splitScreen = enabled))
            }
        }
    }

    fun setSendRemindersToDiscordPreference(send: Boolean) {
        viewModelScope.launch {
            userPrefs.setSendRemindersToDiscord(send)
            updateRemotePreferences { prefs ->
                prefs.copy(general = prefs.general.copy(sendRemindersToDiscord = send))
            }
        }
    }

    fun setDynamicColorPreference(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setDynamicColorEnabled(enabled)
        }
    }

    fun reloadPreferences() {
        // No-op: DataStore flows are reactive and auto-update
    }
}
