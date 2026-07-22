package com.oreoexperience.notes.ui.servicio

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class MonthChartDataTest {

    @Test
    fun `label for January is Ene`() {
        val chart = MonthChartData(2025, Calendar.JANUARY, 10.0, 5, 3)
        assertEquals("Ene", chart.label)
    }

    @Test
    fun `label for December is Dic`() {
        val chart = MonthChartData(2025, Calendar.DECEMBER, 8.0, 2, 1)
        assertEquals("Dic", chart.label)
    }
}
