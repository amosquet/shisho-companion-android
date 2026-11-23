package com.shishocompanion.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.shishocompanion.R
import com.shishocompanion.data.CreateNoteRequest
import com.shishocompanion.data.NoteItem
import com.shishocompanion.data.ReadingListItem
import com.shishocompanion.ui.components.NoteFormScreen

@Composable
fun EditNoteScreen(
    item: NoteItem,
    onSave: (CreateNoteRequest) -> Unit,
    onNavigateBack: () -> Unit,
    isSplitScreen: Boolean = false,
    books: List<ReadingListItem> = emptyList()
) {
    NoteFormScreen(
        screenTitle = stringResource(R.string.edit_note),
        owner = item.owner,
        initialItem = item,
        showArchiveToggle = true,
        books = books,
        initialBookId = item.book,
        onSave = onSave,
        onNavigateBack = onNavigateBack,
        isSplitScreen = isSplitScreen
    )
}
