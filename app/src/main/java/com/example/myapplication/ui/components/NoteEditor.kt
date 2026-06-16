package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@Composable
fun NoteEditor(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lines = text.split("\n")
    val checkboxRegex = Regex("""^-\s*\[([ xX]?)\]\s*(.*)""")
    
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() }
    var lineToFocus by remember { mutableStateOf<Int?>(null) }
    
    LaunchedEffect(lineToFocus, lines.size) {
        lineToFocus?.let { index ->
            focusRequesters[index]?.requestFocus()
            lineToFocus = null
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        lines.forEachIndexed { index, line ->
            val matchResult = checkboxRegex.find(line)
            val fr = focusRequesters.getOrPut(index) { FocusRequester() }
            
            if (matchResult != null) {
                val isChecked = matchResult.groupValues[1].equals("x", ignoreCase = true)
                val content = matchResult.groupValues[2]

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checked ->
                            val marker = if (checked) "- [x] " else "- [ ] "
                            val newLines = lines.toMutableList()
                            newLines[index] = marker + content
                            onTextChange(newLines.joinToString("\n"))
                        }
                    )
                    
                    BasicTextField(
                        value = content,
                        onValueChange = { newContent ->
                            val newLines = lines.toMutableList()
                            if (newContent.contains("\n")) {
                                val parts = newContent.split("\n", limit = 2)
                                val marker = if (isChecked) "- [x] " else "- [ ] "
                                newLines[index] = marker + parts[0]
                                newLines.add(index + 1, "- [ ] " + parts[1])
                                lineToFocus = index + 1
                            } else {
                                val marker = if (isChecked) "- [x] " else "- [ ] "
                                newLines[index] = marker + newContent
                            }
                            onTextChange(newLines.joinToString("\n"))
                        },
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp).focusRequester(fr),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                }
            } else {
                BasicTextField(
                    value = line,
                    onValueChange = { newContent ->
                        val newLines = lines.toMutableList()
                        if (newContent.contains("\n")) {
                            val parts = newContent.split("\n", limit = 2)
                            newLines[index] = parts[0]
                            newLines.add(index + 1, parts[1])
                            lineToFocus = index + 1
                        } else {
                            newLines[index] = newContent
                        }
                        onTextChange(newLines.joinToString("\n"))
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).focusRequester(fr),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }
        }
    }
}
