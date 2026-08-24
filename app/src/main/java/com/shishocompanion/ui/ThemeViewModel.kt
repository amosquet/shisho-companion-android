package com.shishocompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shishocompanion.data.ReadingListRepository
import com.shishocompanion.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val repository: ReadingListRepository,
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

    fun setThemePreference(theme: String) {
        viewModelScope.launch {
            userPrefs.setThemePreference(theme)
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
        viewModelScope.launch {
            userPrefs.setTimeFormatPreference(format)
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
        viewModelScope.launch {
            userPrefs.setDateFormatPreference(format)
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
        viewModelScope.launch {
            userPrefs.setLanguagePreference(lang)
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
        viewModelScope.launch {
            userPrefs.setSplitScreenEnabled(enabled)
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
        viewModelScope.launch {
            userPrefs.setSendRemindersToDiscord(send)
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

    fun setDynamicColorPreference(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setDynamicColorEnabled(enabled)
        }
    }

    fun reloadPreferences() {
        // No-op: DataStore flows are reactive and auto-update
    }
}
