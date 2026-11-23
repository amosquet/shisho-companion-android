package com.shishocompanion.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.shishocompanion.R
import com.shishocompanion.data.ReadingListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookLinkDropdown(
    books: List<ReadingListItem>,
    selectedBookId: String,
    onBookSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (books.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val selectedTitle = books.find { it.id == selectedBookId }?.title
        ?: stringResource(R.string.no_linked_book)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedTitle,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.linked_book)) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.no_linked_book)) },
                onClick = {
                    onBookSelected("")
                    expanded = false
                }
            )
            books.forEach { book ->
                DropdownMenuItem(
                    text = { Text(book.title) },
                    onClick = {
                        onBookSelected(book.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
