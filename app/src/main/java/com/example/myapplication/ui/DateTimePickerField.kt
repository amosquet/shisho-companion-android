package com.example.myapplication.ui

import androidx.compose.foundation.clickable
import com.example.myapplication.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    use24HourTime: Boolean = false
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val parsedCalendar = remember(value) {
        if (value.isBlank()) null
        else {
            try {
                val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", Locale.US)
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val date = parser.parse(value)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.time = date
                    cal
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    val effectiveCalendar = parsedCalendar ?: Calendar.getInstance()

    val initialDateMillis = remember(effectiveCalendar) {
        val localCal = Calendar.getInstance()
        localCal.timeInMillis = effectiveCalendar.timeInMillis
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCal.set(Calendar.YEAR, localCal.get(Calendar.YEAR))
        utcCal.set(Calendar.MONTH, localCal.get(Calendar.MONTH))
        utcCal.set(Calendar.DAY_OF_MONTH, localCal.get(Calendar.DAY_OF_MONTH))
        utcCal.set(Calendar.HOUR_OF_DAY, 0)
        utcCal.set(Calendar.MINUTE, 0)
        utcCal.set(Calendar.SECOND, 0)
        utcCal.set(Calendar.MILLISECOND, 0)
        utcCal.timeInMillis
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
    val timePickerState = rememberTimePickerState(
        initialHour = effectiveCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = effectiveCalendar.get(Calendar.MINUTE),
        is24Hour = use24HourTime
    )

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormatter = remember(use24HourTime) { 
        SimpleDateFormat(if (use24HourTime) "HH:mm" else "h:mm a", Locale.getDefault()) 
    }

    val displayDate = dateFormatter.format(effectiveCalendar.time)
    val displayTime = if (parsedCalendar != null) timeFormatter.format(effectiveCalendar.time) else "Select Time"

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
                    label = { Text("Date") },
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
                    label = { Text("Time") },
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
                        val utcCalForDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        utcCalForDate.timeInMillis = millis

                        val newCal = Calendar.getInstance()
                        newCal.timeInMillis = effectiveCalendar.timeInMillis
                        newCal.set(Calendar.YEAR, utcCalForDate.get(Calendar.YEAR))
                        newCal.set(Calendar.MONTH, utcCalForDate.get(Calendar.MONTH))
                        newCal.set(Calendar.DAY_OF_MONTH, utcCalForDate.get(Calendar.DAY_OF_MONTH))

                        val pbFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", Locale.US)
                        pbFormatter.timeZone = TimeZone.getTimeZone("UTC")
                        onValueChange(pbFormatter.format(newCal.time))
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
                    val newCal = Calendar.getInstance()
                    newCal.timeInMillis = effectiveCalendar.timeInMillis
                    newCal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    newCal.set(Calendar.MINUTE, timePickerState.minute)
                    newCal.set(Calendar.SECOND, 0)
                    newCal.set(Calendar.MILLISECOND, 0)

                    val pbFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", Locale.US)
                    pbFormatter.timeZone = TimeZone.getTimeZone("UTC")
                    onValueChange(pbFormatter.format(newCal.time))
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
