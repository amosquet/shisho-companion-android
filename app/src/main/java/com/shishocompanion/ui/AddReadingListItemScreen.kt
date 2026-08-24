package com.shishocompanion.ui

import androidx.compose.foundation.layout.*
import com.shishocompanion.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shishocompanion.data.CreateReadingListItemRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import coil.compose.AsyncImage
import android.net.Uri
import android.provider.OpenableColumns
import com.shishocompanion.data.VolumeInfo
import androidx.compose.material.icons.filled.Search
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReadingListItemScreen(
    onSave: (CreateReadingListItemRequest) -> Unit,
    onNavigateBack: () -> Unit,
    onSearchBooks: (title: String?, author: String?, isbn: String?, onResult: (List<VolumeInfo>) -> Unit) -> Unit,
    isSplitScreen: Boolean = false
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var isbn by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var isSearchingTitle by remember { mutableStateOf(false) }
    var isSearchingAuthor by remember { mutableStateOf(false) }
    var isSearchingIsbn by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<VolumeInfo>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        coverUri = uri
    }

    searchResults?.let { books ->
        BookSelectionDialog(
            books = books,
            onSelect = { volumeInfo ->
                volumeInfo.title?.let { title = it }
                volumeInfo.authors?.joinToString(", ")?.let { author = it }
                volumeInfo.description?.let { description = it }
                val foundIsbn = volumeInfo.industryIdentifiers?.find { it.type == "ISBN_13" }?.identifier
                    ?: volumeInfo.industryIdentifiers?.find { it.type == "ISBN_10" }?.identifier
                if (!foundIsbn.isNullOrBlank()) {
                    isbn = foundIsbn
                }
                searchResults = null
            },
            onDismiss = { searchResults = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_to_reading_list)) },
                navigationIcon = {
                    if (!isSplitScreen) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
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
                singleLine = true,
                trailingIcon = {
                    if (isSearchingTitle) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(4.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = {
                            if (title.isNotBlank()) {
                                isSearchingTitle = true
                                onSearchBooks(title, author.takeIf { it.isNotBlank() }, null) { results ->
                                    isSearchingTitle = false
                                    if (results.isEmpty()) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.no_books_found))
                                        }
                                    } else {
                                        searchResults = results
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search by Title")
                        }
                    }
                }
            )
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text(stringResource(R.string.author)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (isSearchingAuthor) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(4.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = {
                            if (author.isNotBlank() || title.isNotBlank()) {
                                isSearchingAuthor = true
                                onSearchBooks(title.takeIf { it.isNotBlank() }, author.takeIf { it.isNotBlank() }, null) { results ->
                                    isSearchingAuthor = false
                                    if (results.isEmpty()) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.no_books_found))
                                        }
                                    } else {
                                        searchResults = results
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search by Author")
                        }
                    }
                }
            )
            OutlinedTextField(
                value = isbn,
                onValueChange = { isbn = it },
                label = { Text(stringResource(R.string.isbn)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (isSearchingIsbn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(4.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = {
                            if (isbn.isNotBlank()) {
                                isSearchingIsbn = true
                                onSearchBooks(null, null, isbn) { results ->
                                    isSearchingIsbn = false
                                    if (results.isEmpty()) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.no_books_found))
                                        }
                                    } else if (results.size == 1) {
                                        val volumeInfo = results[0]
                                        volumeInfo.title?.let { title = it }
                                        volumeInfo.authors?.joinToString(", ")?.let { author = it }
                                        volumeInfo.description?.let { description = it }
                                    } else {
                                        searchResults = results
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search by ISBN")
                        }
                    }
                }
            )
            var expanded by remember { mutableStateOf(false) }
            val statusOptions = listOf("planned", "reading", "read", "dropped")

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = status,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.status)) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    statusOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                status = option
                                expanded = false
                            }
                        )
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DatePickerField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = "Start Date",
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = "End Date",
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.description_optional)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Text(stringResource(R.string.select_cover_image))
                }
                
                coverUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Cover Image Preview",
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (title.isNotBlank() || isbn.isNotBlank()) {
                        var coverBytes: ByteArray? = null
                        var coverFilename: String? = null
                        coverUri?.let { uri ->
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                coverBytes = stream.readBytes()
                            }
                            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    if (nameIndex != -1) {
                                        coverFilename = cursor.getString(nameIndex)
                                    }
                                }
                            }
                        }

                        val req = CreateReadingListItemRequest(
                            title = title,
                            url = "",
                            description = description,
                            author = author,
                            status = status,
                            isbn = isbn,
                            startDate = startDate,
                            endDate = endDate,
                            coverBytes = coverBytes,
                            coverFilename = coverFilename
                        )
                        onSave(req)
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() || isbn.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
