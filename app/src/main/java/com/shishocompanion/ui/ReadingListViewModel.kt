package com.shishocompanion.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shishocompanion.data.CreateReadingListItemRequest
import com.shishocompanion.data.ReadingListItem
import com.shishocompanion.data.ReadingListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ReadingListViewModel(
    private val repository: ReadingListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReadingListUiState>(ReadingListUiState.Loading)
    val uiState: StateFlow<ReadingListUiState> = _uiState.asStateFlow()

    init {
        loadReadingList()
    }

    fun loadReadingList() {
        viewModelScope.launch {
            repository.getReadingList()
                .catch { e ->
                    _uiState.value = ReadingListUiState.Error(e.message ?: "Unknown error occurred")
                }
                .collect { items ->
                    _uiState.value = ReadingListUiState.Success(items)
                }
        }
    }

    fun syncReadingList() {
        viewModelScope.launch {
            repository.sync()
        }
    }

    fun addItem(request: CreateReadingListItemRequest) {
        viewModelScope.launch {
            repository.addItem(request)
            // Room Flow automatically pushes new items to the UI
        }
    }

    fun updateItem(id: String, request: CreateReadingListItemRequest) {
        viewModelScope.launch {
            repository.updateItem(id, request)
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            repository.deleteItem(id)
        }
    }

    fun getItemById(id: String): ReadingListItem? {
        val currentState = uiState.value
        if (currentState is ReadingListUiState.Success) {
            return currentState.items.find { it.id == id }
        }
        return null
    }
}

sealed class ReadingListUiState {
    object Loading : ReadingListUiState()
    data class Success(val items: List<ReadingListItem>) : ReadingListUiState()
    data class Error(val message: String) : ReadingListUiState()
}
