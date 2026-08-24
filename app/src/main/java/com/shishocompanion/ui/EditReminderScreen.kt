package com.shishocompanion.ui

import androidx.compose.foundation.layout.*
import com.shishocompanion.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shishocompanion.data.CreateReminderRequest
import com.shishocompanion.data.ReminderItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderScreen(
    item: ReminderItem,
    use24HourTime: Boolean,
    onSave: (CreateReminderRequest) -> Unit,
    onNavigateBack: () -> Unit,
    isSplitScreen: Boolean = false,
    dateFormatPreference: String = "SYSTEM"
) {
    var reminderText by remember { mutableStateOf(item.reminderText) }
    var remindAt by remember { mutableStateOf(item.remindAt) }
    var isSent by remember { mutableStateOf(item.isSent) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_reminder)) },
                navigationIcon = {
                    if (!isSplitScreen) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = reminderText,
                onValueChange = { reminderText = it },
                label = { Text(stringResource(R.string.reminder_text)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            DateTimePickerField(
                value = remindAt,
                onValueChange = { remindAt = it },
                label = "Remind At",
                use24HourTime = use24HourTime,
                dateFormatPreference = dateFormatPreference,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = isSent, onCheckedChange = { isSent = it })
                Text(stringResource(R.string.sent))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onSave(
                        CreateReminderRequest(
                            owner = item.owner,
                            reminderText = reminderText,
                            remindAt = remindAt,
                            isSent = isSent
                        )
                    )
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save_changes))
            }
        }
    }
}
