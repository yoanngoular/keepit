package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class WeeklyReminderTimeZoneTest {
    @Test
    fun `preserve local wall time through the spring transition and resolve the missing hour`() {
        val rule = WeeklyReminderRule(
            startDate = LocalDate(2026, 3, 22),
            time = LocalTime(2, 30),
            zoneId = "Europe/Paris",
            intervalWeeks = 1,
            weekdays = setOf(DayOfWeek.SUNDAY),
            count = 3,
        )

        assertEquals(
            listOf(
                "2026-03-22T01:30:00Z",
                "2026-03-29T01:30:00Z",
                "2026-04-05T00:30:00Z",
            ).map(Instant::parse),
            rule.occurrences().map(ReminderOccurrence::at),
        )
    }
}
