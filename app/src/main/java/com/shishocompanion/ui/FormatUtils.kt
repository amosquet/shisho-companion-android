package com.shishocompanion.ui

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun formatReminderTime(rawTime: String, use24HourTime: Boolean = false, dateFormat: String = "SYSTEM"): String {
    if (rawTime.isBlank()) return rawTime
    
    val timeFormat = if (use24HourTime) "HH:mm z" else "h:mm a z"
    val datePattern = when (dateFormat) {
        "DD_MM_YYYY" -> "dd-MM-yyyy"
        "MM_DD_YYYY" -> "MM-dd-yyyy"
        "YYYY_MM_DD" -> "yyyy-MM-dd"
        else -> "MMM dd, yyyy"
    }
    val formatPattern = "$datePattern $timeFormat"
    
    return try {
        // Handle "2022-01-01 00:00:00.000Z"
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(rawTime)
        if (date != null) {
            val formatter = SimpleDateFormat(formatPattern, Locale.getDefault())
            formatter.timeZone = TimeZone.getDefault()
            formatter.format(date)
        } else {
            rawTime
        }
    } catch (e: Exception) {
        try {
             // Fallback for missing milliseconds or other standard formats
             val fallbackParser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'", Locale.US)
             fallbackParser.timeZone = TimeZone.getTimeZone("UTC")
             val date = fallbackParser.parse(rawTime)
             if (date != null) {
                val formatter = SimpleDateFormat(formatPattern, Locale.getDefault())
                formatter.timeZone = TimeZone.getDefault()
                formatter.format(date)
             } else {
                rawTime
             }
        } catch (e2: Exception) {
            rawTime
        }
    }
}

fun formatDateOnly(rawTime: String, dateFormat: String = "SYSTEM", useLocalTimezone: Boolean = true): String {
    if (rawTime.isBlank()) return rawTime
    
    val formatPattern = when (dateFormat) {
        "DD_MM_YYYY" -> "dd-MM-yyyy"
        "MM_DD_YYYY" -> "MM-dd-yyyy"
        "YYYY_MM_DD" -> "yyyy-MM-dd"
        else -> "MMM dd, yyyy"
    }
    
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'", Locale.US)
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(rawTime)
        if (date != null) {
            val formatter = SimpleDateFormat(formatPattern, Locale.getDefault())
            if (useLocalTimezone) {
                formatter.timeZone = TimeZone.getDefault()
            } else {
                formatter.timeZone = TimeZone.getTimeZone("UTC")
            }
            formatter.format(date)
        } else {
            rawTime
        }
    } catch (e: Exception) {
        try {
             val fallbackParser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss'Z'", Locale.US)
             fallbackParser.timeZone = TimeZone.getTimeZone("UTC")
             val date = fallbackParser.parse(rawTime)
             if (date != null) {
                val formatter = SimpleDateFormat(formatPattern, Locale.getDefault())
                if (useLocalTimezone) {
                    formatter.timeZone = TimeZone.getDefault()
                } else {
                    formatter.timeZone = TimeZone.getTimeZone("UTC")
                }
                formatter.format(date)
             } else {
                rawTime
             }
        } catch (e2: Exception) {
            try {
                val dateOnlyParser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val date = dateOnlyParser.parse(rawTime)
                if (date != null) {
                    val formatter = SimpleDateFormat(formatPattern, Locale.getDefault())
                    formatter.format(date)
                } else {
                    rawTime
                }
            } catch (e3: Exception) {
                rawTime
            }
        }
    }
}
