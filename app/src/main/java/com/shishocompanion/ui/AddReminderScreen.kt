package com.shishocompanion.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.shishocompanion.R
import com.shishocompanion.data.CreateReminderRequest
import com.shishocompanion.data.ReadingListItem
import com.shishocompanion.ui.components.ReminderFormScreen

@Composable
fun AddReminderScreen(
    owner: String,
    use24HourTime: Boolean,
    onSave: (CreateReminderRequest) -> Unit,
    onNavigateBack: () -> Unit,
    isSplitScreen: Boolean = false,
    dateFormatPreference: String = "SYSTEM",
    books: List<ReadingListItem> = emptyList(),
    initialBookId: String = "",
    initialText: String = ""
) {
    ReminderFormScreen(
        screenTitle = stringResource(R.string.add_reminder),
        owner = owner,
        use24HourTime = use24HourTime,
        dateFormatPreference = dateFormatPreference,
        initialItem = null,
        showSentToggle = false,
        books = books,
        initialBookId = initialBookId,
        initialText = initialText,
        saveButtonLabel = stringResource(R.string.save),
        onSave = onSave,
        onNavigateBack = onNavigateBack,
        isSplitScreen = isSplitScreen
    )
}
