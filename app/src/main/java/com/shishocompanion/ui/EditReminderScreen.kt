package com.shishocompanion.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.shishocompanion.R
import com.shishocompanion.data.CreateReminderRequest
import com.shishocompanion.data.ReadingListItem
import com.shishocompanion.data.ReminderItem
import com.shishocompanion.ui.components.ReminderFormScreen

@Composable
fun EditReminderScreen(
    item: ReminderItem,
    use24HourTime: Boolean,
    onSave: (CreateReminderRequest) -> Unit,
    onNavigateBack: () -> Unit,
    isSplitScreen: Boolean = false,
    dateFormatPreference: String = "SYSTEM",
    books: List<ReadingListItem> = emptyList()
) {
    ReminderFormScreen(
        screenTitle = stringResource(R.string.edit_reminder),
        owner = item.owner,
        use24HourTime = use24HourTime,
        dateFormatPreference = dateFormatPreference,
        initialItem = item,
        showSentToggle = true,
        books = books,
        initialBookId = item.book,
        initialText = item.reminderText,
        saveButtonLabel = stringResource(R.string.save_changes),
        onSave = onSave,
        onNavigateBack = onNavigateBack,
        isSplitScreen = isSplitScreen
    )
}
