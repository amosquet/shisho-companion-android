package com.shishocompanion.ui

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

class FormatUtilsTest {

    @Test
    fun parsePocketBaseInstant_handlesSpaceAndIsoFormats() {
        val spaceInstant = parsePocketBaseInstant("2026-05-24 10:00:00.000Z")
        val isoInstant = parsePocketBaseInstant("2026-05-24T10:00:00.000Z")
        val noZInstant = parsePocketBaseInstant("2026-05-24 10:00:00")

        assertNotNull(spaceInstant)
        assertEquals(Instant.parse("2026-05-24T10:00:00.000Z"), spaceInstant)
        assertEquals(spaceInstant, isoInstant)
        assertEquals(Instant.parse("2026-05-24T10:00:00Z"), noZInstant)
        assertNull(parsePocketBaseInstant("   "))
        assertNull(parsePocketBaseInstant("invalid-date"))
    }

    @Test
    fun formatDateOnly_formatsSpaceSeparatedAndDateOnlyStrings() {
        assertEquals("24-05-2026", formatDateOnly("2026-05-24 00:00:00.000Z", "DD_MM_YYYY", useLocalTimezone = false))
        assertEquals("05-24-2026", formatDateOnly("2026-05-24T00:00:00.000Z", "MM_DD_YYYY", useLocalTimezone = false))
        assertEquals("2026-05-24", formatDateOnly("2026-05-24", "YYYY_MM_DD", useLocalTimezone = false))
    }

    @Test
    fun formatReminderTime_formatsSpaceSeparatedTimestamp() {
        val originalTz = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val formatted24 = formatReminderTime("2026-05-24 14:30:00.000Z", use24HourTime = true, dateFormat = "YYYY_MM_DD")
            assertTrue(formatted24.startsWith("2026-05-24 14:30"))
            val formatted12 = formatReminderTime("2026-05-24 14:30:00.000Z", use24HourTime = false, dateFormat = "MM_DD_YYYY")
            assertTrue(formatted12.startsWith("05-24-2026 2:30"))
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun parseNoteMarkdownLine_appliesInlineStyles() {
        val result = parseNoteMarkdownLine("Hello **bold** *italic* <u>under</u> ~~strike~~")
        assertEquals("Hello bold italic under strike", result.text)
        assertTrue(result.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(result.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
        assertTrue(result.spanStyles.any { it.item.textDecoration == TextDecoration.Underline })
        assertTrue(result.spanStyles.any { it.item.textDecoration == TextDecoration.LineThrough })
    }
}
