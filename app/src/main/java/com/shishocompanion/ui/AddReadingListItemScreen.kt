package com.shishocompanion.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.shishocompanion.R
import com.shishocompanion.data.BookSearchResult
import com.shishocompanion.data.CreateReadingListItemRequest
import com.shishocompanion.ui.components.ReadingListItemFormScreen

@Composable
fun AddReadingListItemScreen(
    onSave: (CreateReadingListItemRequest) -> Unit,
    onNavigateBack: () -> Unit,
    onSearchBooks: (title: String?, author: String?, isbn: String?, onResult: (BookSearchResult) -> Unit) -> Unit,
    isSplitScreen: Boolean = false
) {
    ReadingListItemFormScreen(
        screenTitle = stringResource(R.string.add_to_reading_list),
        initialItem = null,
        onSave = onSave,
        onNavigateBack = onNavigateBack,
        onSearchBooks = onSearchBooks,
        isSplitScreen = isSplitScreen
    )
}
