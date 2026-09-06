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
}
