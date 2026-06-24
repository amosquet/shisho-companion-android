package com.shishocompanion.ui

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun formatReminderTime(rawTime: String, use24HourTime: Boolean = false): String {
    if (rawTime.isBlank()) return rawTime
    
    val formatPattern = if (use24HourTime) "MMM dd, yyyy HH:mm z" else "MMM dd, yyyy h:mm a z"
    
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

fun formatDateOnly(rawTime: String): String {
    if (rawTime.isBlank()) return rawTime
    
    val formatPattern = "MMM dd, yyyy"
    
    return try {
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
