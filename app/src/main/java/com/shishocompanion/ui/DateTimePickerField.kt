package com.shishocompanion.ui

import androidx.compose.foundation.clickable
import com.shishocompanion.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    use24HourTime: Boolean = false,
    dateFormatPreference: String = "SYSTEM"
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val parsedZDT = remember(value) {
        if (value.isBlank()) null
        else {
            try {
                Instant.parse(value.replace(" ", "T")).atZone(ZoneId.systemDefault())
            } catch (e: Exception) {
                null
            }
        }
    }

    val effectiveZDT = parsedZDT ?: ZonedDateTime.now(ZoneId.systemDefault())

    val initialDateMillis = remember(effectiveZDT) {
        val utcDate = effectiveZDT.toLocalDate().atStartOfDay(ZoneOffset.UTC)
        utcDate.toInstant().toEpochMilli()
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
    val timePickerState = rememberTimePickerState(
        initialHour = effectiveZDT.hour,
        initialMinute = effectiveZDT.minute,
        is24Hour = use24HourTime
    )

    val dateFormatter = remember(dateFormatPreference) { 
        val pattern = when (dateFormatPreference) {
            "DD_MM_YYYY" -> "dd-MM-yyyy"
            "MM_DD_YYYY" -> "MM-dd-yyyy"
            "YYYY_MM_DD" -> "yyyy-MM-dd"
            else -> "MMM dd, yyyy"
        }
        DateTimeFormatter.ofPattern(pattern)
    }
    val timeFormatter = remember(use24HourTime) { 
        DateTimeFormatter.ofPattern(if (use24HourTime) "HH:mm" else "h:mm a")
    }

    val displayDate = effectiveZDT.format(dateFormatter)
    val displayTime = if (parsedZDT != null) effectiveZDT.format(timeFormatter) else "Select Time"

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = displayDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.date)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = displayTime,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.time)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showTimePicker = true }
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedLocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        
                        val newZDT = effectiveZDT.withYear(selectedLocalDate.year)
                            .withMonth(selectedLocalDate.monthValue)
                            .withDayOfMonth(selectedLocalDate.dayOfMonth)

                        val pbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)
                        onValueChange(pbFormatter.format(newZDT))
                    }
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    val newZDT = effectiveZDT.withHour(timePickerState.hour)
                        .withMinute(timePickerState.minute)
                        .withSecond(0)
                        .withNano(0)

                    val pbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)
                    onValueChange(pbFormatter.format(newZDT))
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}
