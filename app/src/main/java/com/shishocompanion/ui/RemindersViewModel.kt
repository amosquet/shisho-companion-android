package com.shishocompanion.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shishocompanion.data.CreateReminderRequest
import com.shishocompanion.data.ReminderItem
import com.shishocompanion.data.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

data class RemindersUiState(
    val reminders: List<ReminderItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class RemindersViewModel @Inject constructor(
    application: Application,
    private val repository: ReminderRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RemindersUiState())
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()

    private val _selectedReminderIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedReminderIds: StateFlow<Set<String>> = _selectedReminderIds.asStateFlow()

    private var previousItems = emptyList<ReminderItem>()

    init {
        viewModelScope.launch {
            repository.getReminders().collect { items ->
                _uiState.value = _uiState.value.copy(reminders = items)
                
                val currentIds = items.map { it.id }.toSet()
                val removedItems = previousItems.filter { it.id !in currentIds }
                
                removedItems.forEach { item ->
                    com.shishocompanion.notifications.NotificationHelper.cancelAlarm(application, item.id)
                }

                items.forEach { item ->
                    if (!item.isSent) {
                        com.shishocompanion.notifications.NotificationHelper.scheduleAlarm(application, item)
                    } else {
                        com.shishocompanion.notifications.NotificationHelper.cancelAlarm(application, item.id)
                    }
                }
                
                previousItems = items
            }
        }
    }

    fun syncReminders() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                repository.sync()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun addReminder(req: CreateReminderRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                repository.addReminder(req)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateReminder(id: String, req: CreateReminderRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                repository.updateReminder(id, req)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                repository.deleteReminder(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun toggleReminderSelection(id: String) {
        _selectedReminderIds.value = _selectedReminderIds.value.toMutableSet().apply {
            if (contains(id)) remove(id) else add(id)
        }
    }

    fun clearReminderSelection() {
        _selectedReminderIds.value = emptySet()
    }

    fun deleteSelectedReminders() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                _selectedReminderIds.value.forEach { id ->
                    repository.deleteReminder(id)
                }
                clearReminderSelection()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun archiveReminder(id: String) {
        val reminder = getReminderById(id)
        if (reminder != null) {
            updateReminder(id, CreateReminderRequest(
                userId = reminder.userId,
                reminderText = reminder.reminderText,
                remindAt = reminder.remindAt,
                isSent = !reminder.isSent
            ))
        }
    }

    fun getReminderById(id: String): ReminderItem? {
        return _uiState.value.reminders.find { it.id == id }
    }
}
