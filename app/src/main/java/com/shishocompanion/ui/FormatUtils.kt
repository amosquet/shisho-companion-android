package com.shishocompanion.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import java.time.Instant
import java.time.LocalDate
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

fun parsePocketBaseInstant(rawTime: String): Instant? {
    if (rawTime.isBlank()) return null
    val normalized = rawTime.trim().replace(" ", "T")
    return try {
        Instant.parse(normalized)
    } catch (_: Exception) {
        try {
            if (!normalized.endsWith("Z") && !normalized.contains("+")) {
                Instant.parse("${normalized}Z")
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

fun formatReminderTime(rawTime: String, use24HourTime: Boolean = false, dateFormat: String = "SYSTEM"): String {
    if (rawTime.isBlank()) return rawTime

    val timePattern = if (use24HourTime) "HH:mm z" else "h:mm a z"
    return try {
        val instant = parsePocketBaseInstant(rawTime) ?: return rawTime
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
    } catch (_: Exception) {
        rawTime
    }
}

fun formatDateOnly(rawTime: String, dateFormat: String = "SYSTEM", useLocalTimezone: Boolean = true): String {
    if (rawTime.isBlank()) return rawTime

    val formatter = getDateFormatter(dateFormat)
    return try {
        val instant = parsePocketBaseInstant(rawTime)
        if (instant != null) {
            val zone = if (useLocalTimezone) ZoneId.systemDefault() else ZoneId.of("UTC")
            instant.atZone(zone).format(formatter)
        } else {
            val localDate = LocalDate.parse(rawTime.trim().take(10))
            localDate.format(formatter)
        }
    } catch (_: Exception) {
        rawTime
    }
}

private val markdownTokenRegex = Regex("""(\*\*[^*]+\*\*|~~[^~]+~~|<u>.*?</u>|\*[^*]+\*|_[^_]+_)""")

fun parseNoteMarkdownLine(line: String): AnnotatedString {
    if (line.isEmpty()) return AnnotatedString("")
    return buildAnnotatedString {
        var currentIndex = 0
        for (match in markdownTokenRegex.findAll(line)) {
            if (match.range.first > currentIndex) {
                append(line.substring(currentIndex, match.range.first))
            }
            val token = match.value
            when {
                token.startsWith("**") && token.endsWith("**") && token.length > 4 -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(parseNoteMarkdownLine(token.substring(2, token.length - 2)))
                    pop()
                }
                token.startsWith("~~") && token.endsWith("~~") && token.length > 4 -> {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(parseNoteMarkdownLine(token.substring(2, token.length - 2)))
                    pop()
                }
                token.startsWith("<u>") && token.endsWith("</u>") && token.length > 7 -> {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                    append(parseNoteMarkdownLine(token.substring(3, token.length - 4)))
                    pop()
                }
                ((token.startsWith("*") && token.endsWith("*")) || (token.startsWith("_") && token.endsWith("_"))) && token.length > 2 -> {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(parseNoteMarkdownLine(token.substring(1, token.length - 1)))
                    pop()
                }
                else -> append(token)
            }
            currentIndex = match.range.last + 1
        }
        if (currentIndex < line.length) {
            append(line.substring(currentIndex))
        }
    }
}
