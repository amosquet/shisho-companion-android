package com.shishocompanion.ui

import androidx.compose.foundation.background
import com.shishocompanion.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shishocompanion.data.ReadingListItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.shishocompanion.data.CreateReadingListItemRequest
import com.shishocompanion.data.NoteItem
import com.shishocompanion.data.ReminderItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    item: ReadingListItem,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onNavigateBack: () -> Unit,
    isSplitScreen: Boolean = false,
    dateFormatPreference: String = "SYSTEM",
    linkedNotes: List<NoteItem> = emptyList(),
    linkedReminders: List<ReminderItem> = emptyList(),
    onAddNoteClick: () -> Unit = {},
    onAddReminderClick: () -> Unit = {},
    onQuickUpdate: ((CreateReadingListItemRequest) -> Unit)? = null
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.title) },
                navigationIcon = {
                    if (!isSplitScreen) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (item.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = stringResource(R.string.book_cover),
                    modifier = Modifier
                        .height(300.dp)
                        .width(200.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
            
            if (item.author.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.by_author_format, item.author),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (onQuickUpdate != null || item.rating > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (star in 1..5) {
                        val filled = star <= item.rating
                        if (onQuickUpdate != null) {
                            IconButton(
                                onClick = {
                                    val newRating = if (item.rating == star) 0 else star
                                    onQuickUpdate(
                                        CreateReadingListItemRequest(
                                            owner = item.owner,
                                            title = item.title,
                                            url = item.url,
                                            description = item.description,
                                            imageUrl = item.imageUrl,
                                            author = item.author,
                                            status = item.status,
                                            publishDate = item.publishDate,
                                            isbn = item.isbn,
                                            startDate = item.startDate,
                                            endDate = item.endDate,
                                            completed = item.completed,
                                            currentPage = item.currentPage,
                                            totalPages = item.totalPages,
                                            rating = newRating
                                        )
                                    )
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                    contentDescription = "$star",
                                    tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (filled) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (item.totalPages > 0 || onQuickUpdate != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (item.totalPages > 0) {
                        val progress = (item.currentPage.toFloat() / item.totalPages.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.totalPages > 0) {
                            Text(
                                text = stringResource(R.string.pages_progress_format, item.currentPage, item.totalPages),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                        if (onQuickUpdate != null) {
                            TextButton(onClick = { showProgressDialog = true }) {
                                Text(stringResource(R.string.update_progress))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.status.isNotEmpty()) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text(item.status.replaceFirstChar { it.uppercase() }) }
                    )
                }
                if (item.publishDate.isNotEmpty()) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text(item.publishDate) }
                    )
                }
            }

            if (item.isbn.isNotEmpty() || item.startDate.isNotEmpty() || item.endDate.isNotEmpty() || item.completed.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (item.isbn.isNotEmpty()) {
                        Text(stringResource(R.string.isbn_format, item.isbn), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                    }
                    if (item.startDate.isNotEmpty()) {
                        Text(stringResource(R.string.started_format, formatDateOnly(item.startDate, dateFormatPreference, false)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                    }
                    if (item.endDate.isNotEmpty()) {
                        Text(stringResource(R.string.finished_format, formatDateOnly(item.endDate, dateFormatPreference, false)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                    } else if (item.completed.isNotEmpty()) {
                        Text(stringResource(R.string.finished_format, item.completed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (item.description.isNotEmpty()) {
                var isExpanded by remember { mutableStateOf(false) }
                
                Text(
                    text = stringResource(R.string.description_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                ) {
                    Text(
                        text = AnnotatedString.fromHtml(item.description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(if (isExpanded) R.string.show_less else R.string.read_more),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddNoteClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.add_note))
                }
                OutlinedButton(
                    onClick = onAddReminderClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.remind_me_to_read), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (linkedNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.linked_notes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                linkedNotes.forEach { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (note.text.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = parseNoteMarkdownLine(note.text.lineSequence().firstOrNull() ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            if (linkedReminders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.linked_reminders),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                linkedReminders.forEach { reminder ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(reminder.reminderText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (reminder.remindAt.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatReminderTime(reminder.remindAt, dateFormat = dateFormatPreference),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProgressDialog && onQuickUpdate != null) {
        var pageInput by remember(item.currentPage) { mutableStateOf(item.currentPage.toString()) }
        var totalInput by remember(item.totalPages) { mutableStateOf(item.totalPages.toString()) }
        AlertDialog(
            onDismissRequest = { showProgressDialog = false },
            title = { Text(stringResource(R.string.update_progress)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.current_page)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = totalInput,
                        onValueChange = { totalInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.total_pages)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newPage = pageInput.toIntOrNull() ?: item.currentPage
                    val newTotal = totalInput.toIntOrNull() ?: item.totalPages
                    onQuickUpdate(
                        CreateReadingListItemRequest(
                            owner = item.owner,
                            title = item.title,
                            url = item.url,
                            description = item.description,
                            imageUrl = item.imageUrl,
                            author = item.author,
                            status = item.status,
                            publishDate = item.publishDate,
                            isbn = item.isbn,
                            startDate = item.startDate,
                            endDate = item.endDate,
                            completed = item.completed,
                            currentPage = newPage,
                            totalPages = newTotal,
                            rating = item.rating
                        )
                    )
                    showProgressDialog = false
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showProgressDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_book)) },
            text = { Text(stringResource(R.string.delete_book_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick()
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
