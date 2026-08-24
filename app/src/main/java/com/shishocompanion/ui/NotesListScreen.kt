package com.shishocompanion.ui

import androidx.compose.foundation.clickable
import com.shishocompanion.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shishocompanion.data.NoteItem
import com.shishocompanion.ui.components.CustomPullToRefreshBox
import com.shishocompanion.ui.components.CustomSwipeToDismissBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    uiState: NotesUiState,
    hasUnreadAnnouncements: Boolean,
    selectedNoteIds: Set<String>,
    onSyncClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onArchiveSelected: () -> Unit,
    onDeleteNote: (String) -> Unit,
    onArchiveNote: (String) -> Unit,
    dateFormatPreference: String = "SYSTEM",
    swipeActionPreference: String = "DELETE"
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val inSelectionMode = selectedNoteIds.isNotEmpty()

    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (inSelectionMode) "${selectedNoteIds.size} selected" else stringResource(R.string.notes)) },
                navigationIcon = {
                    if (inSelectionMode) {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear selection")
                        }
                    }
                },
                actions = {
                    if (inSelectionMode) {
                        IconButton(onClick = onArchiveSelected) {
                            Icon(Icons.Filled.Archive, contentDescription = "Archive selected notes")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete selected notes")
                        }
                    } else {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_notes))
                        }
                        IconButton(onClick = onArchiveClick) {
                            Icon(Icons.Filled.Archive, contentDescription = "Archived Notes")
                        }
                        IconButton(onClick = onSyncClick) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.sync))
                        }
                        IconButton(onClick = onSettingsClick) {
                            if (hasUnreadAnnouncements) {
                                BadgedBox(badge = { Badge() }) {
                                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                                }
                            } else {
                                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                            }
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
            CustomPullToRefreshBox(
                isRefreshing = uiState is NotesUiState.Loading,
                onRefresh = onSyncClick,
                modifier = Modifier.fillMaxSize()
            ) {
                when (uiState) {
                    is NotesUiState.Loading -> {
                        // handled by CustomPullToRefreshBox
                    }
                    is NotesUiState.Error -> {
                        Text(
                            text = uiState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is NotesUiState.Success -> {
                        val filteredNotes = uiState.items.filter { !it.archived }
                        if (filteredNotes.isEmpty()) {
                            Text(
                                text = "You have no notes.",
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
                                items(filteredNotes, key = { it.id }) { item ->
                                    val isSelected = selectedNoteIds.contains(item.id)
                                    CustomSwipeToDismissBox(
                                        swipeActionPreference = swipeActionPreference,
                                        onDismiss = { onDeleteNote(item.id) },
                                        onArchive = { onArchiveNote(item.id) }
                                    ) {
                                        NoteItemCard(
                                            item = item,
                                            isSelected = isSelected,
                                            dateFormatPreference = dateFormatPreference,
                                            onClick = { 
                                                if (inSelectionMode) onToggleSelection(item.id)
                                                else onItemClick(item.id)
                                            },
                                            onLongClick = { onToggleSelection(item.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isSearchActive) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = { },
                            expanded = isSearchActive,
                            onExpandedChange = { isSearchActive = it },
                            placeholder = { Text(stringResource(R.string.search_notes_placeholder)) },
                            leadingIcon = {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    searchQuery = ""
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                                }
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_search))
                                    }
                                }
                            }
                        )
                    },
                    expanded = isSearchActive,
                    onExpandedChange = { isSearchActive = it },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (uiState is NotesUiState.Success) {
                        val searchResults = remember(uiState.items, searchQuery) {
                            if (searchQuery.isBlank()) emptyList()
                            else uiState.items.filter {
                                !it.archived && (
                                    it.title.contains(searchQuery, ignoreCase = true) ||
                                    it.text.contains(searchQuery, ignoreCase = true)
                                )
                            }
                        }
                        if (searchQuery.isBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.search_notes_prompt),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        } else if (searchResults.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_matching_notes),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(searchResults, key = { it.id }) { item ->
                                    NoteItemCard(
                                        item = item,
                                        isSelected = false,
                                        dateFormatPreference = dateFormatPreference,
                                        onClick = {
                                            isSearchActive = false
                                            searchQuery = ""
                                            onItemClick(item.id)
                                        },
                                        onLongClick = {}
                                    )
                                }
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
            title = { Text(stringResource(R.string.delete_selected_notes)) },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItemCard(item: NoteItem, isSelected: Boolean, dateFormatPreference: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.text.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                val lines = item.text.split("\n").take(3)
                val checkboxRegex = Regex("""^-\s*\[([ xX]?)\]\s*(.*)""")
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    lines.forEach { line ->
                        val matchResult = checkboxRegex.find(line)
                        if (matchResult != null) {
                            val isChecked = matchResult.groupValues[1].equals("x", ignoreCase = true)
                            val content = matchResult.groupValues[2]
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (item.text.split("\n").size > 3) {
                        Text(
                            text = "...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            if (item.attachment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${item.attachment.size} attachment${if (item.attachment.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (item.updated.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Updated: ${formatDateOnly(item.updated, dateFormatPreference)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
