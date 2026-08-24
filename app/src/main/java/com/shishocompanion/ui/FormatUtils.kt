package com.shishocompanion.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun getDateFormatter(dateFormat: String = "SYSTEM"): DateTimeFormatter {
    return when (dateFormat) {
        "DD_MM_YYYY" -> DateTimeFormatter.ofPattern("dd-MM-yyyy")
        "MM_DD_YYYY" -> DateTimeFormatter.ofPattern("MM-dd-yyyy")
        "YYYY_MM_DD" -> DateTimeFormatter.ofPattern("yyyy-MM-dd")
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }
}

fun formatReminderTime(rawTime: String, use24HourTime: Boolean = false, dateFormat: String = "SYSTEM"): String {
    if (rawTime.isBlank()) return rawTime
    
    val timePattern = if (use24HourTime) "HH:mm z" else "h:mm a z"
    return try {
        val instant = Instant.parse(rawTime)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        if (dateFormat == "SYSTEM") {
            val datePart = zonedDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            val timePart = zonedDateTime.format(DateTimeFormatter.ofPattern(timePattern))
            "$datePart $timePart"
        } else {
            val datePattern = when (dateFormat) {
                "DD_MM_YYYY" -> "dd-MM-yyyy"
                "MM_DD_YYYY" -> "MM-dd-yyyy"
                "YYYY_MM_DD" -> "yyyy-MM-dd"
                else -> "MMM dd, yyyy"
            }
            val formatter = DateTimeFormatter.ofPattern("$datePattern $timePattern")
            zonedDateTime.format(formatter)
        }
    } catch (e: Exception) {
        rawTime
    }
}

fun formatDateOnly(rawTime: String, dateFormat: String = "SYSTEM", useLocalTimezone: Boolean = true): String {
    if (rawTime.isBlank()) return rawTime
    
    return try {
        val instant = Instant.parse(rawTime)
        val zone = if (useLocalTimezone) ZoneId.systemDefault() else ZoneId.of("UTC")
        val zonedDateTime = instant.atZone(zone)
        val formatter = getDateFormatter(dateFormat)
        zonedDateTime.format(formatter)
    } catch (e: Exception) {
        rawTime
    }
}
