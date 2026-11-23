package com.shishocompanion.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingListSortingTest {

    @Test
    fun sortReadingList_putsPlannedLastAndSortsByLatestDate() {
        val planned = ReadingListItem(
            id = "1",
            title = "Planned Book",
            status = "planned",
            created = "2026-05-30 00:00:00.000Z"
        )
        val finished = ReadingListItem(
            id = "2",
            title = "Finished Book",
            status = "read",
            endDate = "2026-05-20 00:00:00.000Z"
        )
        val readingNewer = ReadingListItem(
            id = "3",
            title = "Reading Newer",
            status = "reading",
            startDate = "2026-05-25 00:00:00.000Z"
        )

        val sorted = ReadingListRepository.sortReadingList(listOf(planned, finished, readingNewer))
        assertEquals(listOf("3", "2", "1"), sorted.map { it.id })
    }

    @Test
    fun sortReadingList_breaksTiesByPublishDateDescThenTitleAsc() {
        val bookB = ReadingListItem(
            id = "b",
            title = "Beta",
            status = "read",
            endDate = "2026-05-20 00:00:00.000Z",
            publishDate = "2020"
        )
        val bookA = ReadingListItem(
            id = "a",
            title = "Alpha",
            status = "read",
            endDate = "2026-05-20 00:00:00.000Z",
            publishDate = "2020"
        )
        val bookNewerPub = ReadingListItem(
            id = "c",
            title = "Gamma",
            status = "read",
            endDate = "2026-05-20 00:00:00.000Z",
            publishDate = "2024"
        )

        val sorted = ReadingListRepository.sortReadingList(listOf(bookB, bookA, bookNewerPub))
        assertEquals(listOf("c", "a", "b"), sorted.map { it.id })
    }
}
