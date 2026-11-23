package com.shishocompanion.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.shishocompanion.R
import com.shishocompanion.data.CreateNoteRequest
import com.shishocompanion.data.ReadingListItem
import com.shishocompanion.ui.components.NoteFormScreen

@Composable
fun AddNoteScreen(
    owner: String,
    onSave: (CreateNoteRequest) -> Unit,
    onNavigateBack: () -> Unit,
    isSplitScreen: Boolean = false,
    books: List<ReadingListItem> = emptyList(),
    initialBookId: String = ""
) {
    NoteFormScreen(
        screenTitle = stringResource(R.string.add_note),
        owner = owner,
        initialItem = null,
        showArchiveToggle = false,
        books = books,
        initialBookId = initialBookId,
        onSave = onSave,
        onNavigateBack = onNavigateBack,
        isSplitScreen = isSplitScreen
    )
}
