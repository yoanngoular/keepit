package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class WeeklyReminderRuleTest {
    private val rule = WeeklyReminderRule(
        startDate = LocalDate(2026, 12, 30),
        time = LocalTime(9, 0),
        zoneId = "UTC",
        intervalWeeks = 2,
        weekdays = setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        count = 6,
    )

    @Test
    fun `start at the first selected day on or after the start date and keep the ISO week anchor`() {
        val expected = listOf(
            "2026-12-30T09:00:00Z",
            "2027-01-01T09:00:00Z",
            "2027-01-11T09:00:00Z",
            "2027-01-13T09:00:00Z",
            "2027-01-15T09:00:00Z",
            "2027-01-25T09:00:00Z",
        ).mapIndexed { index, instant -> ReminderOccurrence(index + 1, Instant.parse(instant)) }

        assertEquals(expected, rule.occurrences())
    }

    @Test
    fun `start on the next anchored week when the first week has no remaining selected day`() {
        val sundayStart = rule.copy(startDate = LocalDate(2027, 1, 3), count = 1)
        assertEquals(
            listOf(ReminderOccurrence(1, Instant.parse("2027-01-11T09:00:00Z"))),
            sundayStart.occurrences(),
        )
    }

    @Test
    fun `resolve each occurrence at its local time and stop once the count is reached`() {
        val fixedOffset = rule.copy(zoneId = "+02:00", count = 2)
        assertEquals(
            listOf(
                ReminderOccurrence(1, Instant.parse("2026-12-30T07:00:00Z")),
                ReminderOccurrence(2, Instant.parse("2027-01-01T07:00:00Z")),
            ),
            fixedOffset.occurrences(),
        )
    }

    @Test
    fun `reject invalid recurrence fields`() {
        assertFailsWith<IllegalArgumentException> { rule.copy(intervalWeeks = 0) }
        assertFailsWith<IllegalArgumentException> { rule.copy(intervalWeeks = 121) }
        assertFailsWith<IllegalArgumentException> { rule.copy(weekdays = emptySet()) }
        assertFailsWith<IllegalArgumentException> { rule.copy(count = 0) }
        assertFailsWith<IllegalArgumentException> { rule.copy(count = 101) }
        assertFailsWith<IllegalArgumentException> { rule.copy(zoneId = "Invalid/Zone") }
    }
}
