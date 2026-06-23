package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import com.example.myapplication.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.CreateReadingListItemRequest
import com.example.myapplication.data.ReadingListItem
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import coil.compose.AsyncImage
import android.net.Uri
import android.provider.OpenableColumns

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReadingListItemScreen(
    item: ReadingListItem,
    onSave: (CreateReadingListItemRequest) -> Unit,
    onNavigateBack: () -> Unit,
    isSplitScreen: Boolean = false
) {
    var title by remember { mutableStateOf(item.title) }
    var author by remember { mutableStateOf(item.author) }
    var isbn by remember { mutableStateOf(item.isbn) }
    var status by remember { mutableStateOf(item.status) }
    var startDate by remember { mutableStateOf(item.startDate) }
    var endDate by remember { mutableStateOf(item.endDate) }
    var description by remember { mutableStateOf(item.description) }
    var coverUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        coverUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_reading_list_item)) },
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
                singleLine = true
            )
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text(stringResource(R.string.author)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = isbn,
                onValueChange = { isbn = it },
                label = { Text(stringResource(R.string.isbn)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
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
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
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
                
                val displayUri = coverUri ?: item.imageUrl
                if (displayUri.toString().isNotEmpty()) {
                    AsyncImage(
                        model = displayUri,
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
                            url = item.url,
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
