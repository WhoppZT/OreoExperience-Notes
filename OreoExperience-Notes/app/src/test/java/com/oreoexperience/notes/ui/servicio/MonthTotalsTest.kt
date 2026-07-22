package com.oreoexperience.notes.ui.servicio

import org.junit.Assert.*
import org.junit.Test

class MonthTotalsTest {

    @Test
    fun `empty totals are zero by default`() {
        val t = MonthTotals()
        assertEquals(0.0, t.totalHours, 0.001)
        assertEquals(0, t.totalRevisits)
        assertEquals(0, t.totalPublications)
        assertEquals(0, t.totalVideos)
        assertEquals(0, t.totalStudies)
        assertEquals(0, t.totalDays)
    }

    @Test
    fun `totals sum correctly`() {
        val t = MonthTotals(
            totalHours = 12.5,
            totalRevisits = 8,
            totalPublications = 3,
            totalVideos = 2,
            totalStudies = 5,
            totalDays = 10,
        )
        assertEquals(12.5, t.totalHours, 0.001)
        assertEquals(8, t.totalRevisits)
        assertEquals(3, t.totalPublications)
        assertEquals(2, t.totalVideos)
        assertEquals(5, t.totalStudies)
        assertEquals(10, t.totalDays)
    }

    @Test
    fun `totals with mixed fractional hours`() {
        val t = MonthTotals(totalHours = 0.5 + 1.0 + 2.75)
        assertEquals(4.25, t.totalHours, 0.001)
    }
}
