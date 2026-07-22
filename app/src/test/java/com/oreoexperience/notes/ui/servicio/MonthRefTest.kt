package com.oreoexperience.notes.ui.servicio

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class MonthRefTest {

    private fun today(): MonthRef {
        val c = Calendar.getInstance()
        return MonthRef(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
    }

    @Test
    fun `today returns current month and year`() {
        val expected = today()
        val result = MonthRef.today()
        assertEquals(expected.year, result.year)
        assertEquals(expected.month, result.month)
    }

    @Test
    fun `previous from January goes to December of previous year`() {
        val jan = MonthRef(2025, Calendar.JANUARY)
        val prev = jan.previous()
        assertEquals(2024, prev.year)
        assertEquals(Calendar.DECEMBER, prev.month)
    }

    @Test
    fun `previous from March goes to February same year`() {
        val mar = MonthRef(2025, Calendar.MARCH)
        val prev = mar.previous()
        assertEquals(2025, prev.year)
        assertEquals(Calendar.FEBRUARY, prev.month)
    }

    @Test
    fun `next from December goes to January of next year`() {
        val dec = MonthRef(2025, Calendar.DECEMBER)
        val next = dec.next()
        assertEquals(2026, next.year)
        assertEquals(Calendar.JANUARY, next.month)
    }

    @Test
    fun `next from June goes to July same year`() {
        val jun = MonthRef(2025, Calendar.JUNE)
        val next = jun.next()
        assertEquals(2025, next.year)
        assertEquals(Calendar.JULY, next.month)
    }

    @Test
    fun `previous then next returns to original`() {
        val original = MonthRef(2025, Calendar.MAY)
        val result = original.previous().next()
        assertEquals(original, result)
    }

    @Test
    fun `next then previous returns to original`() {
        val original = MonthRef(2025, Calendar.AUGUST)
        val result = original.next().previous()
        assertEquals(original, result)
    }

    @Test
    fun `today is not null and valid`() {
        val t = MonthRef.today()
        assertTrue(t.year > 2000)
        assertTrue(t.month in 0..11)
    }
}
