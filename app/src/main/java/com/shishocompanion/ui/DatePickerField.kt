package com.shishocompanion.ui

import androidx.compose.foundation.clickable
import com.shishocompanion.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val initialDateMillis = remember(value) {
        if (value.isBlank()) null
        else {
            try {
                val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", Locale.US)
                parser.timeZone = TimeZone.getTimeZone("UTC")
                parser.parse(value)?.time
            } catch (e: Exception) {
                try {
                    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    parser.timeZone = TimeZone.getTimeZone("UTC")
                    parser.parse(value)?.time
                } catch (e2: Exception) {
                    null
                }
            }
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
    
    val displayValue = remember(value) {
        if (value.isBlank()) "" else formatDateOnly(value, "YYYY_MM_DD", false)
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.select_date))
            }
        )
        // Clickable overlay to intercept all clicks on the field
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true }
        )
    }

    if (showDialog) {
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("yyyy-MM-dd 00:00:00.000'Z'", Locale.US)
                        formatter.timeZone = TimeZone.getTimeZone("UTC")
                        onValueChange(formatter.format(Date(millis)))
                    }
                    showDialog = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
