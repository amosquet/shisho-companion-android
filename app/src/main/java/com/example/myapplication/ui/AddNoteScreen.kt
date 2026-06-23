package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import com.example.myapplication.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import com.example.myapplication.data.CreateNoteRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteScreen(
    userId: String,
    onSave: (CreateNoteRequest) -> Unit,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var attachmentUris by remember { mutableStateOf<List<String>>(emptyList()) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val newUris = (attachmentUris + uris.map { it.toString() }).take(5)
            attachmentUris = newUris
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_note)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val req = CreateNoteRequest(
                                userId = userId,
                                title = title,
                                text = text,
                                attachment = attachmentUris,
                                archived = false
                            )
                            onSave(req)
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
                com.example.myapplication.ui.components.NoteEditor(
                    text = text,
                    onTextChange = { text = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = { launcher.launch("*/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.add_attachments_format, attachmentUris.size))
            }
            if (attachmentUris.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Attachments:", style = MaterialTheme.typography.titleSmall)
                    attachmentUris.forEachIndexed { index, uri ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Preview",
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
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { 
                                val newUris = attachmentUris.toMutableList()
                                newUris.removeAt(index)
                                attachmentUris = newUris
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove Attachment")
                            }
                        }
                    }
                }
            }
        }
    }
}
