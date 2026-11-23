package com.shishocompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shishocompanion.data.AuthRepository
import com.shishocompanion.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPrefs: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiscordLinkUiState>(DiscordLinkUiState.Idle)
    val uiState: StateFlow<DiscordLinkUiState> = _uiState.asStateFlow()

    val linkedDiscordId: StateFlow<String> = userPrefs.discordDisplayId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val googleBooksApiKey: StateFlow<String> = userPrefs.googleBooksApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val swipeActionPreference: StateFlow<String> = userPrefs.swipeAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DELETE")

    init {
        viewModelScope.launch {
            val me = authRepository.getMe()
            if (me.isSuccess) {
                val fetchedDiscordId = me.getOrNull()?.discordId ?: ""
                userPrefs.setDiscordDisplayId(fetchedDiscordId)
            }
        }
    }

    fun requestDiscordPin(discordId: String) {
        viewModelScope.launch {
            _uiState.value = DiscordLinkUiState.Loading
            val result = authRepository.requestDiscordPin(discordId)
            if (result.isSuccess) {
                _uiState.value = DiscordLinkUiState.PinRequested(discordId)
            } else {
                _uiState.value = DiscordLinkUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun verifyDiscordPin(discordId: String, pin: String) {
        viewModelScope.launch {
            _uiState.value = DiscordLinkUiState.PinRequested(discordId, isVerifying = true)
            val result = authRepository.linkDiscord(discordId, pin)
            if (result.isSuccess) {
                userPrefs.setDiscordDisplayId(discordId)
                _uiState.value = DiscordLinkUiState.Success
            } else {
                _uiState.value = DiscordLinkUiState.PinRequested(
                    discordId = discordId,
                    isVerifying = false,
                    verifyError = result.exceptionOrNull()?.message ?: "Invalid PIN"
                )
            }
        }
    }

    fun unlinkDiscord() {
        viewModelScope.launch {
            _uiState.value = DiscordLinkUiState.Loading
            val result = authRepository.unlinkDiscord()
            if (result.isSuccess) {
                userPrefs.setDiscordDisplayId("")
                _uiState.value = DiscordLinkUiState.Idle
            } else {
                _uiState.value = DiscordLinkUiState.Error(result.exceptionOrNull()?.message ?: "Failed to unlink Discord")
            }
        }
    }

    fun resetState() {
        _uiState.value = DiscordLinkUiState.Idle
    }

    fun updateGoogleBooksApiKey(key: String) {
        viewModelScope.launch {
            userPrefs.setGoogleBooksApiKey(key)
        }
    }

    fun updateSwipeActionPreference(action: String) {
        viewModelScope.launch {
            userPrefs.setSwipeAction(action)
        }
    }
}

sealed class DiscordLinkUiState {
    object Idle : DiscordLinkUiState()
    object Loading : DiscordLinkUiState()
    data class PinRequested(
        val discordId: String,
        val isVerifying: Boolean = false,
        val verifyError: String? = null
    ) : DiscordLinkUiState()
    object Success : DiscordLinkUiState()
    data class Error(val message: String) : DiscordLinkUiState()
}
