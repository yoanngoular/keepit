package com.ygoular.keepit.reminder

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class MonthlyReminderTimeZoneTest {
    @Test
    fun `preserve wall time across summer and winter offsets`() {
        val rule = MonthlyReminderRule(
            startDate = LocalDate(2026, 1, 1),
            time = LocalTime(9, 0),
            zoneId = "Europe/Paris",
            intervalMonths = 2,
            ordinal = 2,
            weekday = DayOfWeek.TUESDAY,
            count = 6,
        )

        assertEquals(
            listOf(
                "2026-01-13T08:00:00Z",
                "2026-03-10T08:00:00Z",
                "2026-05-12T07:00:00Z",
                "2026-07-14T07:00:00Z",
                "2026-09-08T07:00:00Z",
                "2026-11-10T08:00:00Z",
            ).map(Instant::parse),
            rule.occurrences().map(ReminderOccurrence::at),
        )
    }

    @Test
    fun `move nonexistent wall time forward by the clock gap`() {
        val rule = transitionSunday(startDate = LocalDate(2026, 3, 1), ordinal = 5)

        assertEquals(listOf(ReminderOccurrence(1, Instant.parse("2026-03-29T01:30:00Z"))), rule.occurrences())
    }

    @Test
    fun `use earlier instant when clocks repeat a wall time`() {
        val rule = transitionSunday(startDate = LocalDate(2026, 10, 1), ordinal = 4)

        assertEquals(listOf(ReminderOccurrence(1, Instant.parse("2026-10-25T00:30:00Z"))), rule.occurrences())
    }

    private fun transitionSunday(startDate: LocalDate, ordinal: Int) = MonthlyReminderRule(
        startDate = startDate,
        time = LocalTime(2, 30),
        zoneId = "Europe/Paris",
        intervalMonths = 1,
        ordinal = ordinal,
        weekday = DayOfWeek.SUNDAY,
        count = 1,
    )
}
