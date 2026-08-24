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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    owner: String,
    use24HourTime: Boolean,
    onSave: (CreateReminderRequest) -> Unit,
    onNavigateBack: () -> Unit,
    isSplitScreen: Boolean = false,
    dateFormatPreference: String = "SYSTEM"
) {
    var reminderText by remember { mutableStateOf("") }
    var remindAt by remember { mutableStateOf("") } // expecting ISO like 2026-05-24 10:00:00.000Z for now

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_reminder)) },
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
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onSave(
                        CreateReminderRequest(
                            owner = owner,
                            reminderText = reminderText,
                            remindAt = remindAt,
                            isSent = false
                        )
                    )
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
