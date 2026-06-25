package com.shishocompanion.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shishocompanion.data.CreateNoteRequest
import com.shishocompanion.data.NoteItem
import com.shishocompanion.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class NotesViewModel(
    application: Application,
    private val repository: NoteRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<NotesUiState>(NotesUiState.Loading)
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private val _selectedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedNoteIds: StateFlow<Set<String>> = _selectedNoteIds.asStateFlow()

    init {
        loadNotes()
    }

    fun loadNotes() {
        viewModelScope.launch {
            repository.getNotes()
                .catch { e ->
                    _uiState.value = NotesUiState.Error(e.message ?: "Unknown error occurred")
                }
                .collect { items ->
                    _uiState.value = NotesUiState.Success(items)
                }
        }
    }

    fun syncNotes() {
        viewModelScope.launch {
            repository.sync()
        }
    }

    fun addNote(request: CreateNoteRequest) {
        viewModelScope.launch {
            val finalPaths = request.attachment.mapNotNull { copyUriToCache(it) }.filter { it.isNotEmpty() }
            repository.addNote(request.copy(attachment = finalPaths))
        }
    }

    fun updateNote(id: String, request: CreateNoteRequest) {
        viewModelScope.launch {
            val finalPaths = request.attachment.mapNotNull { copyUriToCache(it) }.filter { it.isNotEmpty() }
            repository.updateNote(id, request.copy(attachment = finalPaths))
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    fun toggleNoteSelection(id: String) {
        _selectedNoteIds.value = _selectedNoteIds.value.toMutableSet().apply {
            if (contains(id)) remove(id) else add(id)
        }
    }

    fun clearNoteSelection() {
        _selectedNoteIds.value = emptySet()
    }

    fun deleteSelectedNotes() {
        viewModelScope.launch {
            _selectedNoteIds.value.forEach { id ->
                repository.deleteNote(id)
            }
            clearNoteSelection()
        }
    }

    fun archiveNote(id: String) {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is NotesUiState.Success) {
                val note = currentState.items.find { it.id == id }
                if (note != null) {
                    updateNote(id, CreateNoteRequest(
                        userId = note.userId,
                        title = note.title,
                        text = note.text,
                        attachment = note.attachment,
                        archived = !note.archived
                    ))
                }
            }
        }
    }

    fun archiveSelectedNotes() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is NotesUiState.Success) {
                _selectedNoteIds.value.forEach { id ->
                    val note = currentState.items.find { it.id == id }
                    if (note != null && !note.archived) {
                        updateNote(id, CreateNoteRequest(
                            userId = note.userId,
                            title = note.title,
                            text = note.text,
                            attachment = note.attachment,
                            archived = true
                        ))
                    }
                }
            }
            clearNoteSelection()
        }
    }

    fun unarchiveSelectedNotes() {
        viewModelScope.launch {
            val currentState = uiState.value
            if (currentState is NotesUiState.Success) {
                _selectedNoteIds.value.forEach { id ->
                    val note = currentState.items.find { it.id == id }
                    if (note != null && note.archived) {
                        updateNote(id, CreateNoteRequest(
                            userId = note.userId,
                            title = note.title,
                            text = note.text,
                            attachment = note.attachment,
                            archived = false
                        ))
                    }
                }
            }
            clearNoteSelection()
        }
    }

    fun getNoteById(id: String): NoteItem? {
        val currentState = uiState.value
        if (currentState is NotesUiState.Success) {
            return currentState.items.find { it.id == id }
        }
        return null
    }

    fun copyUriToCache(uriString: String): String? {
        if (uriString.isEmpty()) return ""
        if (uriString.startsWith("http") || uriString.startsWith("/")) return uriString // Already remote URL or local file path
        
        try {
            val uri = Uri.parse(uriString)
            val context = getApplication<Application>()
            val contentResolver = context.contentResolver
            
            // Generate a unique filename
            val ext = contentResolver.getType(uri)?.substringAfterLast('/') ?: "tmp"
            val file = File(context.cacheDir, "attachment_${System.currentTimeMillis()}.$ext")
            
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            return file.absolutePath
        } catch (e: Exception) {
            // Exception handled silently
            return null
        }
    }
}

sealed class NotesUiState {
    object Loading : NotesUiState()
    data class Success(val items: List<NoteItem>) : NotesUiState()
    data class Error(val message: String) : NotesUiState()
}
