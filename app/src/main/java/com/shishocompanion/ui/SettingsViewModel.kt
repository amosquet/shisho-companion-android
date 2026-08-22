package com.shishocompanion.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shishocompanion.data.ReadingListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ReadingListRepository,
    private val sharedPrefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiscordLinkUiState>(DiscordLinkUiState.Idle)
    val uiState: StateFlow<DiscordLinkUiState> = _uiState.asStateFlow()

    private val _linkedDiscordId = MutableStateFlow<String>(sharedPrefs.getString("DISCORD_ID", "") ?: "")
    val linkedDiscordId: StateFlow<String> = _linkedDiscordId.asStateFlow()

    private val _googleBooksApiKey = MutableStateFlow<String>(sharedPrefs.getString("GOOGLE_BOOKS_API_KEY", "") ?: "")
    val googleBooksApiKey: StateFlow<String> = _googleBooksApiKey.asStateFlow()

    private val _swipeActionPreference = MutableStateFlow<String>(sharedPrefs.getString("SWIPE_ACTION", "DELETE") ?: "DELETE")
    val swipeActionPreference: StateFlow<String> = _swipeActionPreference.asStateFlow()

    init {
        viewModelScope.launch {
            val me = repository.getMe()
            if (me.isSuccess) {
                val fetchedDiscordId = me.getOrNull()?.discordId ?: ""
                _linkedDiscordId.value = fetchedDiscordId
                sharedPrefs.edit().putString("DISCORD_ID", fetchedDiscordId).apply()
            }
        }
    }

    fun requestDiscordPin(discordId: String) {
        viewModelScope.launch {
            _uiState.value = DiscordLinkUiState.Loading
            val result = repository.requestDiscordPin(discordId)
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
            val result = repository.linkDiscord(discordId, pin)
            if (result.isSuccess) {
                _linkedDiscordId.value = discordId
                sharedPrefs.edit().putString("DISCORD_ID", discordId).apply()
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
            val result = repository.unlinkDiscord()
            if (result.isSuccess) {
                _linkedDiscordId.value = ""
                sharedPrefs.edit().putString("DISCORD_ID", "").apply()
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
        _googleBooksApiKey.value = key
        sharedPrefs.edit().putString("GOOGLE_BOOKS_API_KEY", key).apply()
    }

    fun updateSwipeActionPreference(action: String) {
        _swipeActionPreference.value = action
        sharedPrefs.edit().putString("SWIPE_ACTION", action).apply()
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
