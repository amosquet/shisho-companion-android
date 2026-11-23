package com.shishocompanion.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.shishocompanion.R
import com.shishocompanion.data.BookSearchResult
import com.shishocompanion.data.CreateReadingListItemRequest
import com.shishocompanion.data.ReadingListItem
import com.shishocompanion.ui.components.ReadingListItemFormScreen

@Composable
fun EditReadingListItemScreen(
    item: ReadingListItem,
    onSave: (CreateReadingListItemRequest) -> Unit,
    onNavigateBack: () -> Unit,
    onSearchBooks: ((title: String?, author: String?, isbn: String?, onResult: (BookSearchResult) -> Unit) -> Unit)? = null,
    isSplitScreen: Boolean = false
) {
    ReadingListItemFormScreen(
        screenTitle = stringResource(R.string.edit_reading_list_item),
        initialItem = item,
        onSave = onSave,
        onNavigateBack = onNavigateBack,
        onSearchBooks = onSearchBooks,
        isSplitScreen = isSplitScreen
    )
}
