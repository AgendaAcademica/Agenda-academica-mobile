package com.example.ui.screens

import com.example.data.model.AcademicActivity
import com.example.data.model.ActivityType
import com.example.data.model.Priority
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardCalendarFilterTest {
    private val activities = listOf(
        AcademicActivity(
            id = 1,
            disciplineId = 10,
            title = "Prova de Cálculo",
            activityType = ActivityType.PROVA,
            date = "2026-09-10",
            time = "08:00",
            priority = Priority.URGENTE
        ),
        AcademicActivity(
            id = 2,
            disciplineId = 20,
            title = "Trabalho de Programação",
            activityType = ActivityType.TRABALHO,
            date = "2026-09-10",
            time = "10:00",
            priority = Priority.ALTA
        )
    )

    @Test
    fun `returns all activities when no discipline is selected`() {
        assertEquals(activities, filterCalendarActivities(activities, null))
    }

    @Test
    fun `returns only activities from selected discipline`() {
        assertEquals(listOf(activities.first()), filterCalendarActivities(activities, 10))
    }

    @Test
    fun `calendar week starts on sunday and ends on saturday`() {
        val days = generateCalendarDays(2026, 7, 30, 2026, 8, 12)
        val week = calendarWeekFor(days, "2026-09-08")

        assertEquals("DOM", week.first().dayLabel)
        assertEquals("2026-09-06", week.first().dateStr)
        assertEquals("SÁB", week.last().dayLabel)
        assertEquals("2026-09-12", week.last().dateStr)
    }
}
