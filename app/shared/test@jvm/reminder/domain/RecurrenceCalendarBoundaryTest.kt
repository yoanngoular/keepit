package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toKotlinLocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class RecurrenceCalendarBoundaryTest {
    @Test
    fun `open-ended civil dates stop at the supported calendar boundary`() {
        val last = java.time.LocalDate.MAX.toKotlinLocalDate()
        val weekly = WeeklyReminderRule(last, LocalTime(9, 0), "UTC", 1, setOf(last.dayOfWeek), endDate = null)
        val monthly = MonthlyReminderRule(
            LocalDate(last.year, last.month, 1), LocalTime(9, 0), "UTC", 1, 5, last.dayOfWeek, endDate = null,
        )

        assertEquals(listOf(last), weekly.scheduledDates().take(2).toList())
        assertEquals(listOf(last), monthly.scheduledDates().take(2).toList())
    }
}
