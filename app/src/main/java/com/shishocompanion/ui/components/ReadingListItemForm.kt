package com.shishocompanion.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.shishocompanion.R
import com.shishocompanion.data.BookSearchResult
import com.shishocompanion.data.CreateReadingListItemRequest
import com.shishocompanion.data.ReadingListItem
import com.shishocompanion.data.VolumeInfo
import com.shishocompanion.ui.BookSelectionDialog
import com.shishocompanion.ui.DatePickerField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListItemFormScreen(
    screenTitle: String,
    initialItem: ReadingListItem? = null,
    onSave: (CreateReadingListItemRequest) -> Unit,
    onNavigateBack: () -> Unit,
    onSearchBooks: ((title: String?, author: String?, isbn: String?, onResult: (BookSearchResult) -> Unit) -> Unit)? = null,
    isSplitScreen: Boolean = false
) {
    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var author by remember { mutableStateOf(initialItem?.author ?: "") }
    var isbn by remember { mutableStateOf(initialItem?.isbn ?: "") }
    var status by remember { mutableStateOf(initialItem?.status ?: "") }
    var startDate by remember { mutableStateOf(initialItem?.startDate ?: "") }
    var endDate by remember { mutableStateOf(initialItem?.endDate ?: "") }
    var description by remember { mutableStateOf(initialItem?.description ?: "") }
    var remoteImageUrl by remember { mutableStateOf(initialItem?.imageUrl ?: "") }
    var publishDate by remember { mutableStateOf(initialItem?.publishDate ?: "") }
    var currentPageText by remember { mutableStateOf(if ((initialItem?.currentPage ?: 0) > 0) initialItem!!.currentPage.toString() else "") }
    var totalPagesText by remember { mutableStateOf(if ((initialItem?.totalPages ?: 0) > 0) initialItem!!.totalPages.toString() else "") }
    var rating by remember { mutableIntStateOf(initialItem?.rating ?: 0) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var isSearchingTitle by remember { mutableStateOf(false) }
    var isSearchingAuthor by remember { mutableStateOf(false) }
    var isSearchingIsbn by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<VolumeInfo>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val barcodeScannerOptions = remember {
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A
            )
            .build()
    }
    val barcodeScanner = remember(context, barcodeScannerOptions) {
        GmsBarcodeScanning.getClient(context, barcodeScannerOptions)
    }

    val handleSearchResult: (BookSearchResult, ((VolumeInfo) -> Unit)?) -> Unit = { result, onSingleResult ->
        when (result) {
            is BookSearchResult.NoApiKey -> {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.no_api_key_set))
                }
            }
            is BookSearchResult.Success -> {
                if (result.books.isEmpty()) {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.no_books_found))
                    }
                } else if (result.books.size == 1 && onSingleResult != null) {
                    onSingleResult(result.books[0])
                } else {
                    searchResults = result.books
                }
            }
            is BookSearchResult.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(result.message ?: context.getString(R.string.no_books_found))
                }
            }
        }
    }

    val applyVolumeInfo: (VolumeInfo) -> Unit = { volumeInfo ->
        volumeInfo.title?.let { title = it }
        volumeInfo.authors?.joinToString(", ")?.let { author = it }
        volumeInfo.description?.let { description = it }
        val foundIsbn = volumeInfo.industryIdentifiers?.find { it.type == "ISBN_13" }?.identifier
            ?: volumeInfo.industryIdentifiers?.find { it.type == "ISBN_10" }?.identifier
        if (!foundIsbn.isNullOrBlank()) {
            isbn = foundIsbn
        }
        val thumb = (volumeInfo.imageLinks?.thumbnail ?: volumeInfo.imageLinks?.smallThumbnail)
            ?.replace("http://", "https://")
        if (!thumb.isNullOrBlank()) {
            remoteImageUrl = thumb
        }
        volumeInfo.publishedDate?.let { publishDate = it }
        volumeInfo.pageCount?.let { if (it > 0) totalPagesText = it.toString() }
    }

    val performIsbnSearch: (String) -> Unit = { targetIsbn ->
        if (targetIsbn.isNotBlank() && onSearchBooks != null) {
            isSearchingIsbn = true
            onSearchBooks(null, null, targetIsbn) { result ->
                isSearchingIsbn = false
                handleSearchResult(result) { volumeInfo ->
                    applyVolumeInfo(volumeInfo)
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        coverUri = uri
    }

    searchResults?.let { books ->
        BookSelectionDialog(
            books = books,
            onSelect = { volumeInfo ->
                applyVolumeInfo(volumeInfo)
                searchResults = null
            },
            onDismiss = { searchResults = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                trailingIcon = if (onSearchBooks != null) {
                    {
                        if (isSearchingTitle) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(4.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = {
                                if (title.isNotBlank()) {
                                    isSearchingTitle = true
                                    onSearchBooks(title, author.takeIf { it.isNotBlank() }, null) { result ->
                                        isSearchingTitle = false
                                        handleSearchResult(result, null)
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search by Title")
                            }
                        }
                    }
                } else null
            )
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text(stringResource(R.string.author)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = if (onSearchBooks != null) {
                    {
                        if (isSearchingAuthor) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(4.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = {
                                if (author.isNotBlank() || title.isNotBlank()) {
                                    isSearchingAuthor = true
                                    onSearchBooks(title.takeIf { it.isNotBlank() }, author.takeIf { it.isNotBlank() }, null) { result ->
                                        isSearchingAuthor = false
                                        handleSearchResult(result, null)
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search by Author")
                            }
                        }
                    }
                } else null
            )
            OutlinedTextField(
                value = isbn,
                onValueChange = { isbn = it },
                label = { Text(stringResource(R.string.isbn)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            barcodeScanner.startScan()
                                .addOnSuccessListener { barcode ->
                                    val rawValue = barcode.rawValue
                                    if (!rawValue.isNullOrBlank()) {
                                        isbn = rawValue
                                        performIsbnSearch(rawValue)
                                    }
                                }
                                .addOnCanceledListener { }
                                .addOnFailureListener {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(context.getString(R.string.barcode_scan_failed))
                                    }
                                }
                        }) {
                            Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(R.string.scan_barcode))
                        }
                        if (onSearchBooks != null) {
                            if (isSearchingIsbn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp).padding(4.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(onClick = { performIsbnSearch(isbn) }) {
                                    Icon(Icons.Filled.Search, contentDescription = "Search by ISBN")
                                }
                            }
                        }
                    }
                }
            )
            var expanded by remember { mutableStateOf(false) }
            val statusOptions = listOf(
                "planned" to stringResource(R.string.status_planned),
                "reading" to stringResource(R.string.status_reading),
                "read" to stringResource(R.string.status_read),
                "dropped" to stringResource(R.string.status_dropped)
            )
            val statusDisplay = statusOptions.find { it.first == status }?.second ?: status

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = statusDisplay,
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
                    statusOptions.forEach { (key, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                status = key
                                val todayStr = Instant.now().atZone(ZoneOffset.UTC)
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd '00:00:00.000Z'"))
                                if (key == "reading" && startDate.isBlank()) {
                                    startDate = todayStr
                                } else if (key == "read") {
                                    if (endDate.isBlank()) endDate = todayStr
                                    val total = totalPagesText.toIntOrNull() ?: 0
                                    val current = currentPageText.toIntOrNull() ?: 0
                                    if (total > 0 && current == 0) currentPageText = total.toString()
                                }
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
                    label = stringResource(R.string.start_date),
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = stringResource(R.string.end_date),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = currentPageText,
                    onValueChange = { currentPageText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.current_page)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = totalPagesText,
                    onValueChange = { totalPagesText = it.filter { ch -> ch.isDigit() } },
                    label = { Text(stringResource(R.string.total_pages)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.rating), style = MaterialTheme.typography.bodyLarge)
                Row {
                    for (star in 1..5) {
                        IconButton(onClick = { rating = if (rating == star) 0 else star }) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = "$star",
                                tint = if (star <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text(stringResource(R.string.select_cover_image))
                }

                val displayUri: Any? = coverUri ?: remoteImageUrl.takeIf { it.isNotEmpty() }
                if (displayUri != null) {
                    AsyncImage(
                        model = displayUri,
                        contentDescription = stringResource(R.string.book_cover),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (!isSaving && (title.isNotBlank() || isbn.isNotBlank())) {
                        isSaving = true
                        scope.launch {
                            val (coverBytes, coverFilename) = withContext(Dispatchers.IO) {
                                var bytes: ByteArray? = null
                                var filename: String? = null
                                coverUri?.let { uri ->
                                    context.contentResolver.openInputStream(uri)?.use { stream ->
                                        bytes = stream.readBytes()
                                    }
                                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                        if (cursor.moveToFirst()) {
                                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                            if (nameIndex != -1) {
                                                filename = cursor.getString(nameIndex)
                                            }
                                        }
                                    }
                                }
                                bytes to filename
                            }

                            val req = CreateReadingListItemRequest(
                                title = title,
                                url = initialItem?.url ?: "",
                                description = description,
                                imageUrl = remoteImageUrl,
                                author = author,
                                status = status,
                                publishDate = publishDate,
                                isbn = isbn,
                                startDate = startDate,
                                endDate = endDate,
                                completed = initialItem?.completed ?: "",
                                currentPage = currentPageText.toIntOrNull() ?: 0,
                                totalPages = totalPagesText.toIntOrNull() ?: 0,
                                rating = rating,
                                coverBytes = coverBytes,
                                coverFilename = coverFilename
                            )
                            onSave(req)
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && (title.isNotBlank() || isbn.isNotBlank())
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
