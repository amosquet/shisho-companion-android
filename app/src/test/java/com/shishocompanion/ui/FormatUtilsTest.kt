package com.shishocompanion.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun testFormatDateOnly_explicitFormats() {
        val isoDate = "2026-08-23T14:30:00Z"
        assertEquals("23-08-2026", formatDateOnly(isoDate, dateFormat = "DD_MM_YYYY", useLocalTimezone = false))
        assertEquals("08-23-2026", formatDateOnly(isoDate, dateFormat = "MM_DD_YYYY", useLocalTimezone = false))
        assertEquals("2026-08-23", formatDateOnly(isoDate, dateFormat = "YYYY_MM_DD", useLocalTimezone = false))
    }

    @Test
    fun testFormatDateOnly_systemFormat() {
        val isoDate = "2026-08-23T14:30:00Z"
        val formatted = formatDateOnly(isoDate, dateFormat = "SYSTEM", useLocalTimezone = false)
        assertNotNull(formatted)
        assert(formatted.isNotBlank())
    }

    @Test
    fun testFormatDateOnly_blankInput() {
        assertEquals("", formatDateOnly(""))
        assertEquals("   ", formatDateOnly("   "))
    }

    @Test
    fun testFormatReminderTime_blankInput() {
        assertEquals("", formatReminderTime(""))
    }

    @Test
    fun testGetDateFormatter() {
        assertNotNull(getDateFormatter("SYSTEM"))
        assertNotNull(getDateFormatter("DD_MM_YYYY"))
        assertNotNull(getDateFormatter("MM_DD_YYYY"))
        assertNotNull(getDateFormatter("YYYY_MM_DD"))
    }
}
