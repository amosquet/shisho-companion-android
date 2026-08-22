package com.shishocompanion.ui

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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
        val instant = Instant.parse(rawTime)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern(formatPattern)
        zonedDateTime.format(formatter)
    } catch (e: Exception) {
        rawTime
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
        val instant = Instant.parse(rawTime)
        val zone = if (useLocalTimezone) ZoneId.systemDefault() else ZoneId.of("UTC")
        val zonedDateTime = instant.atZone(zone)
        val formatter = DateTimeFormatter.ofPattern(formatPattern)
        zonedDateTime.format(formatter)
    } catch (e: Exception) {
        rawTime
    }
}
