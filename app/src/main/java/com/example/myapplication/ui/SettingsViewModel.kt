package com.example.myapplication.ui

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.ReadingListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: ReadingListRepository,
    private val sharedPrefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<EmailLinkUiState>(EmailLinkUiState.Idle)
    val uiState: StateFlow<EmailLinkUiState> = _uiState.asStateFlow()

    private val _linkedEmail = MutableStateFlow<String>(sharedPrefs.getString("USER_EMAIL", "") ?: "")
    val linkedEmail: StateFlow<String> = _linkedEmail.asStateFlow()

    init {
        viewModelScope.launch {
            val me = repository.getMe()
            if (me.isSuccess) {
                val fetchedEmail = me.getOrNull()?.email ?: ""
                _linkedEmail.value = fetchedEmail
                sharedPrefs.edit().putString("USER_EMAIL", fetchedEmail).apply()
            }
        }
    }

    fun requestEmailLink(email: String) {
        viewModelScope.launch {
            _uiState.value = EmailLinkUiState.Loading
            val result = repository.requestEmailLinkPin(email)
            if (result.isSuccess) {
                _uiState.value = EmailLinkUiState.PinRequested(email)
            } else {
                _uiState.value = EmailLinkUiState.Error(result.exceptionOrNull()?.message ?: "Failed to request PIN")
            }
        }
    }

    fun verifyEmailLink(pin: String) {
        viewModelScope.launch {
            _uiState.value = EmailLinkUiState.Loading
            val result = repository.verifyEmailLinkPin(pin)
            if (result.isSuccess) {
                _uiState.value = EmailLinkUiState.Success
                // refresh linked email
                val me = repository.getMe()
                if (me.isSuccess) {
                    val newEmail = me.getOrNull()?.email ?: ""
                    _linkedEmail.value = newEmail
                    sharedPrefs.edit().putString("USER_EMAIL", newEmail).apply()
                }
            } else {
                _uiState.value = EmailLinkUiState.Error(result.exceptionOrNull()?.message ?: "Invalid PIN")
            }
        }
    }

    fun unlinkEmail() {
        viewModelScope.launch {
            _uiState.value = EmailLinkUiState.Loading
            val result = repository.unlinkEmail()
            if (result.isSuccess) {
                _linkedEmail.value = ""
                sharedPrefs.edit().putString("USER_EMAIL", "").apply()
                _uiState.value = EmailLinkUiState.Idle
            } else {
                _uiState.value = EmailLinkUiState.Error(result.exceptionOrNull()?.message ?: "Failed to unlink email")
            }
        }
    }

    fun resetState() {
        _uiState.value = EmailLinkUiState.Idle
    }
}

sealed class EmailLinkUiState {
    object Idle : EmailLinkUiState()
    object Loading : EmailLinkUiState()
    data class PinRequested(val email: String) : EmailLinkUiState()
    object Success : EmailLinkUiState()
    data class Error(val message: String) : EmailLinkUiState()
}
