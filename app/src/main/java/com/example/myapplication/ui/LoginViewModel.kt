package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ReadingListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import android.content.SharedPreferences

class LoginViewModel(
    private val repository: ReadingListRepository,
    private val database: AppDatabase,
    private val sharedPrefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        val savedToken = sharedPrefs.getString("TOKEN", null)
        val savedUserId = sharedPrefs.getString("USER_ID", null)
        if (!savedToken.isNullOrBlank() && !savedUserId.isNullOrBlank()) {
            repository.authToken = savedToken
            repository.userId = savedUserId
            _uiState.value = LoginUiState.Success
        }
    }

    fun requestPin(discordId: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = repository.requestPin(discordId)
            if (result.isSuccess) {
                _uiState.value = LoginUiState.PinRequested(discordId)
            } else {
                _uiState.value = LoginUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun verifyPin(discordId: String, pin: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = repository.verifyPin(discordId, pin)
            if (result.isSuccess) {
                val userId = repository.userId ?: ""
                val token = repository.authToken ?: ""
                
                // Fetch preferences
                val meResult = repository.getMe()
                if (meResult.isSuccess) {
                    val prefs = meResult.getOrNull()?.preferences
                    prefs?.let {
                        sharedPrefs.edit()
                            .putBoolean("DARK_MODE", it.companion.darkMode)
                            .putBoolean("USE_24_HOUR_TIME", it.general.use24HourTime)
                            .apply()
                    }
                }
                
                sharedPrefs.edit()
                    .putString("TOKEN", token)
                    .putString("USER_ID", userId)
                    .apply()
                _uiState.value = LoginUiState.Success
            } else {
                _uiState.value = LoginUiState.Error(result.exceptionOrNull()?.message ?: "Invalid PIN")
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
        }
        sharedPrefs.edit().clear().apply()
        repository.authToken = null
        repository.userId = null
        _uiState.value = LoginUiState.Idle
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class PinRequested(val discordId: String) : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
