package com.shishocompanion.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditor(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val richTextState = rememberRichTextState()
    
    // We want to track if this is the first load
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(text) {
        if (!isInitialized) {
            richTextState.setMarkdown(text)
            isInitialized = true
        }
    }

    // We can observe changes by comparing the resulting markdown.
    // annotatedString triggers on any text or styling change.
    LaunchedEffect(richTextState.annotatedString) {
        if (isInitialized) {
            val newMarkdown = richTextState.toMarkdown()
            // Avoid calling onTextChange if it's strictly equal to what was passed in,
            // to prevent unnecessary loop cycles.
            if (newMarkdown != text) {
                onTextChange(newMarkdown)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Formatting Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
            ) {
                Icon(Icons.Default.FormatBold, contentDescription = "Bold")
            }
            IconButton(
                onClick = { richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
            ) {
                Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
            }
            IconButton(
                onClick = { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }
            ) {
                Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline")
            }
            IconButton(
                onClick = { richTextState.toggleUnorderedList() }
            ) {
                Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List")
            }
        }
        
        RichTextEditor(
            state = richTextState,
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onBackground
            ),
            colors = RichTextEditorDefaults.richTextEditorColors(
                containerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}
