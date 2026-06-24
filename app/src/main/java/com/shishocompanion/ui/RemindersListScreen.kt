package com.shishocompanion.ui

import androidx.compose.foundation.clickable
import com.shishocompanion.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Archive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shishocompanion.data.ReminderItem
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersListScreen(
    uiState: RemindersUiState,
    hasUnreadAnnouncements: Boolean,
    selectedReminderIds: Set<String>,
    use24HourTime: Boolean,
    onSyncClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val inSelectionMode = selectedReminderIds.isNotEmpty()
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { /* handle result if needed */ }
        )
        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (inSelectionMode) "${selectedReminderIds.size} selected" else stringResource(R.string.reminders)) },
                navigationIcon = {
                    if (inSelectionMode) {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear selection")
                        }
                    }
                },
                actions = {
                    if (inSelectionMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete selected reminders")
                        }
                    } else {
                        IconButton(onClick = onArchiveClick) {
                            Icon(Icons.Filled.Archive, contentDescription = "Archive")
                        }
                        IconButton(onClick = onSyncClick) {
                            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.sync_reminders))
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
            val filteredReminders = uiState.reminders.filter { !it.isSent }

            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (filteredReminders.isEmpty()) {
                Text(
                    text = "No pending reminders.",
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
                    items(filteredReminders) { reminder ->
                        val isSelected = selectedReminderIds.contains(reminder.id)
                        ReminderCard(
                            reminder = reminder, 
                            use24HourTime = use24HourTime, 
                            isSelected = isSelected,
                            onClick = { 
                                if (inSelectionMode) onToggleSelection(reminder.id)
                                else onItemClick(reminder.id)
                            },
                            onLongClick = { onToggleSelection(reminder.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_selected_reminders)) },
            text = { Text(stringResource(R.string.delete_multiple_reminders_confirm, selectedReminderIds.size)) },
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
fun ReminderCard(reminder: ReminderItem, use24HourTime: Boolean, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
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
                text = reminder.reminderText,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Remind at: ${formatReminderTime(reminder.remindAt, use24HourTime)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (reminder.isSent) "Status: Sent" else "Status: Pending",
                style = MaterialTheme.typography.labelSmall,
                color = if (reminder.isSent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}
