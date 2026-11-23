package com.shishocompanion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shishocompanion.R
import com.shishocompanion.data.CreateReminderRequest
import com.shishocompanion.data.ReadingListItem
import com.shishocompanion.data.ReminderItem
import com.shishocompanion.ui.DateTimePickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderFormScreen(
    screenTitle: String,
    owner: String,
    use24HourTime: Boolean,
    dateFormatPreference: String = "SYSTEM",
    initialItem: ReminderItem? = null,
    showSentToggle: Boolean = false,
    books: List<ReadingListItem> = emptyList(),
    initialBookId: String = "",
    initialText: String = "",
    saveButtonLabel: String,
    onSave: (CreateReminderRequest) -> Unit,
    onNavigateBack: () -> Unit,
    isSplitScreen: Boolean = false
) {
    var reminderText by remember { mutableStateOf(initialItem?.reminderText ?: initialText) }
    var remindAt by remember { mutableStateOf(initialItem?.remindAt ?: "") }
    var isSent by remember { mutableStateOf(initialItem?.isSent ?: false) }
    var selectedBookId by remember { mutableStateOf(initialItem?.book ?: initialBookId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
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
            BookLinkDropdown(
                books = books,
                selectedBookId = selectedBookId,
                onBookSelected = { selectedBookId = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DateTimePickerField(
                value = remindAt,
                onValueChange = { remindAt = it },
                label = stringResource(R.string.remind_at),
                use24HourTime = use24HourTime,
                dateFormatPreference = dateFormatPreference,
                modifier = Modifier.fillMaxWidth()
            )
            if (showSentToggle) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSent, onCheckedChange = { isSent = it })
                    Text(stringResource(R.string.sent))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onSave(
                        CreateReminderRequest(
                            owner = owner,
                            reminderText = reminderText,
                            remindAt = remindAt,
                            isSent = isSent,
                            book = selectedBookId
                        )
                    )
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = reminderText.isNotBlank() && remindAt.isNotBlank()
            ) {
                Text(saveButtonLabel)
            }
        }
    }
}
