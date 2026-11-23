package com.shishocompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shishocompanion.data.AppDatabase
import com.shishocompanion.data.AuthRepository
import com.shishocompanion.data.ReadingListRepository
import com.shishocompanion.data.NoteRepository
import com.shishocompanion.data.ReminderRepository
import com.shishocompanion.data.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val authRepository: AuthRepository,
    private val repository: ReadingListRepository,
    private val noteRepository: NoteRepository,
    private val reminderRepository: ReminderRepository,
    private val userPrefs: UserPreferencesRepository,
    private val database: AppDatabase,
    private val sharedPrefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _showSessionExpiredDialog = MutableStateFlow(false)
    val showSessionExpiredDialog: StateFlow<Boolean> = _showSessionExpiredDialog.asStateFlow()

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
            
            if (com.shishocompanion.data.TokenUtils.isTokenExpired(savedToken)) {
                _showSessionExpiredDialog.value = true
            } else {
                viewModelScope.launch {
                    val refreshed = authRepository.refreshAuthTokenIfNeeded().getOrDefault(false)
                    if (refreshed) {
                        val newToken = sharedPrefs.getString("TOKEN", null)
                        repository.authToken = newToken
                        noteRepository.authToken = newToken
                        reminderRepository.authToken = newToken
                    }
                    fetchAndSavePreferences()
                }
            }
        } else if (sharedPrefs.getBoolean("IS_GUEST", false)) {
            _uiState.value = LoginUiState.Guest
        }
    }

    fun dismissSessionExpiredDialog() {
        _showSessionExpiredDialog.value = false
    }

    fun login(email: String, pin: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = authRepository.loginWithEmail(email, pin)
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
            val result = authRepository.registerWithEmail(email, pin)
            if (result.isSuccess) {
                onLoginSuccess()
            } else {
                _uiState.value = LoginUiState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    private suspend fun fetchAndSavePreferences() {
        val meResult = authRepository.getMe()
        if (meResult.isSuccess) {
            val me = meResult.getOrNull()
            userPrefs.updateFromRemote(me?.preferences, me?.discordId)
        }
    }

    private suspend fun onLoginSuccess() {
        val userId = authRepository.userId ?: ""
        val token = authRepository.authToken ?: ""

        repository.authToken = token
        repository.userId = userId
        repository.userEmail = authRepository.userEmail
        noteRepository.authToken = token
        noteRepository.userId = userId
        reminderRepository.authToken = token
        reminderRepository.userId = userId
        
        // Fetch preferences
        fetchAndSavePreferences()
            
        // Sync local data created as guest to pocketbase
        repository.sync()
        noteRepository.sync()
        reminderRepository.sync()
        
        _uiState.value = LoginUiState.Success
    }
    
    fun continueAsGuest() {
        sharedPrefs.edit { putBoolean("IS_GUEST", true) }
        _uiState.value = LoginUiState.Guest
    }

    fun hasActiveOrGuestSession(): Boolean {
        return _uiState.value is LoginUiState.Success || _uiState.value is LoginUiState.Guest
    }

    fun prepareReLogin() {
        sharedPrefs.edit {
            remove("TOKEN")
        }
        repository.authToken = null
        noteRepository.authToken = null
        reminderRepository.authToken = null
        _uiState.value = LoginUiState.Idle
    }

    fun logout() {
        com.shishocompanion.notifications.NotificationHelper.cancelAllAlarms(appContext)
        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
        }
        viewModelScope.launch {
            userPrefs.clear()
        }
        sharedPrefs.edit { clear() }
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
