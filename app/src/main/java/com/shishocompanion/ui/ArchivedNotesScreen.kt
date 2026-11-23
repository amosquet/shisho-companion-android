package com.shishocompanion.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import com.shishocompanion.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedNotesScreen(
    uiState: NotesUiState,
    selectedNoteIds: Set<String>,
    onNavigateBack: () -> Unit,
    onItemClick: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onUnarchiveSelected: () -> Unit,
    dateFormatPreference: String = "SYSTEM"
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val inSelectionMode = selectedNoteIds.isNotEmpty()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (inSelectionMode) stringResource(R.string.selected_count_format, selectedNoteIds.size) else stringResource(R.string.archived_notes)) },
                navigationIcon = {
                    if (inSelectionMode) {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear selection")
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (inSelectionMode) {
                        IconButton(onClick = onUnarchiveSelected) {
                            Icon(Icons.Filled.Unarchive, contentDescription = "Unarchive selected notes")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete selected notes")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is NotesUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is NotesUiState.Error -> {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is NotesUiState.Success -> {
                    val filteredNotes = uiState.items.filter { it.archived }
                    
                    if (filteredNotes.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_archived_notes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                NoteItemCard(
                                    item = note, 
                                    isSelected = selectedNoteIds.contains(note.id),
                                    dateFormatPreference = dateFormatPreference,
                                    onClick = { 
                                        if (inSelectionMode) onToggleSelection(note.id)
                                        else onItemClick(note.id)
                                    },
                                    onLongClick = { onToggleSelection(note.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_archived_notes)) },
            text = { Text(stringResource(R.string.delete_multiple_notes_confirm, selectedNoteIds.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteSelected()
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
