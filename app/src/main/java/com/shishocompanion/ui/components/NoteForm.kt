package com.shishocompanion.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shishocompanion.R
import com.shishocompanion.data.CreateNoteRequest
import com.shishocompanion.data.NoteItem
import com.shishocompanion.data.ReadingListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteFormScreen(
    screenTitle: String,
    owner: String,
    initialItem: NoteItem? = null,
    showArchiveToggle: Boolean = false,
    books: List<ReadingListItem> = emptyList(),
    initialBookId: String = "",
    onSave: (CreateNoteRequest) -> Unit,
    onNavigateBack: () -> Unit,
    isSplitScreen: Boolean = false
) {
    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var text by remember { mutableStateOf(initialItem?.text ?: "") }
    var selectedBookId by remember { mutableStateOf(initialItem?.book ?: initialBookId) }
    var attachmentUris by remember { mutableStateOf(initialItem?.attachment ?: emptyList()) }
    var archived by remember { mutableStateOf(initialItem?.archived ?: false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            attachmentUris = (attachmentUris + uris.map { it.toString() }).take(5)
        }
    }

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
                },
                actions = {
                    IconButton(
                        onClick = {
                            onSave(
                                CreateNoteRequest(
                                    owner = owner,
                                    title = title,
                                    text = text,
                                    attachment = attachmentUris,
                                    archived = archived,
                                    book = selectedBookId
                                )
                            )
                            onNavigateBack()
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            BookLinkDropdown(
                books = books,
                selectedBookId = selectedBookId,
                onBookSelected = { selectedBookId = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    val prefix = if (text.isEmpty() || text.endsWith("\n")) "" else "\n"
                    text += "$prefix- [ ] "
                }) {
                    Text(stringResource(R.string.add_list_item_button))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.content),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                NoteEditor(
                    text = text,
                    onTextChange = { text = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showArchiveToggle) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = archived, onCheckedChange = { archived = it })
                    Text(stringResource(R.string.archived))
                }
            }

            Button(
                onClick = { launcher.launch("*/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.add_attachments_format, attachmentUris.size))
            }

            if (attachmentUris.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = stringResource(R.string.attachment), style = MaterialTheme.typography.titleSmall)
                    attachmentUris.forEachIndexed { index, uri ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = stringResource(R.string.attachment),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val filename = uri.substringAfterLast('/').substringBefore('?')
                            Text(
                                text = filename,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = {
                                attachmentUris = attachmentUris.toMutableList().apply { removeAt(index) }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    }
                }
            }
        }
    }
}
