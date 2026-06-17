package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AppDatabase
import com.example.myapplication.data.ReadingListRepository
import com.example.myapplication.data.NoteRepository
import com.example.myapplication.data.ReminderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import android.content.SharedPreferences

class LoginViewModel(
    private val repository: ReadingListRepository,
    private val noteRepository: NoteRepository,
    private val reminderRepository: ReminderRepository,
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
            noteRepository.authToken = savedToken
            noteRepository.userId = savedUserId
            reminderRepository.authToken = savedToken
            reminderRepository.userId = savedUserId
            _uiState.value = LoginUiState.Success
        } else if (sharedPrefs.getBoolean("IS_GUEST", false)) {
            _uiState.value = LoginUiState.Guest
        }
    }

    fun login(email: String, pin: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = repository.loginWithEmail(email, pin)
            if (result.isSuccess) {
                onLoginSuccess()
            } else {
                _uiState.value = LoginUiState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun register(email: String, pin: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = repository.registerWithEmail(email, pin)
            if (result.isSuccess) {
                onLoginSuccess()
            } else {
                _uiState.value = LoginUiState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    private suspend fun onLoginSuccess() {
        val userId = repository.userId ?: ""
        val token = repository.authToken ?: ""
        
        noteRepository.authToken = token
        noteRepository.userId = userId
        reminderRepository.authToken = token
        reminderRepository.userId = userId
        
        // Fetch preferences
        val meResult = repository.getMe()
        if (meResult.isSuccess) {
            val me = meResult.getOrNull()
            val prefs = me?.preferences
            prefs?.let {
                sharedPrefs.edit()
                    .putString("THEME_PREFERENCE", it.companion.theme)
                    .putString("TIME_FORMAT_PREFERENCE", it.general.timeFormat)
                    .putBoolean("SEND_REMINDERS_TO_DISCORD", it.general.sendRemindersToDiscord)
                    .apply()
            }
            sharedPrefs.edit().putString("DISCORD_ID", me?.discordId ?: "").apply()
        }
        
        val email = repository.userEmail ?: ""

        sharedPrefs.edit()
            .putString("TOKEN", token)
            .putString("USER_ID", userId)
            .putString("USER_EMAIL", email)
            .putBoolean("IS_GUEST", false)
            .apply()
            
        // Sync local data created as guest to pocketbase
        repository.sync()
        noteRepository.sync()
        reminderRepository.sync()
        
        _uiState.value = LoginUiState.Success
    }
    
    fun continueAsGuest() {
        sharedPrefs.edit().putBoolean("IS_GUEST", true).apply()
        _uiState.value = LoginUiState.Guest
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
        }
        sharedPrefs.edit().clear().apply()
        repository.authToken = null
        repository.userId = null
        noteRepository.authToken = null
        noteRepository.userId = null
        reminderRepository.authToken = null
        reminderRepository.userId = null
        _uiState.value = LoginUiState.Idle
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    object Guest : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
