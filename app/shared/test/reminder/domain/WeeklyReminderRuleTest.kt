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
        endDate = LocalDate(2027, 1, 25),
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

        assertEquals(expected, rule.occurrences().toList())
    }

    @Test
    fun `start on the next anchored week when the first week has no remaining selected day`() {
        val sundayStart = rule.copy(startDate = LocalDate(2027, 1, 3), endDate = LocalDate(2027, 1, 11))
        assertEquals(
            listOf(ReminderOccurrence(1, Instant.parse("2027-01-11T09:00:00Z"))),
            sundayStart.occurrences().toList(),
        )
    }

    @Test
    fun `resolve each occurrence at its local time and stop after the end date`() {
        val fixedOffset = rule.copy(zoneId = "+02:00", endDate = LocalDate(2027, 1, 10))
        assertEquals(
            listOf(
                ReminderOccurrence(1, Instant.parse("2026-12-30T07:00:00Z")),
                ReminderOccurrence(2, Instant.parse("2027-01-01T07:00:00Z")),
            ),
            fixedOffset.occurrences().toList(),
        )
    }

    @Test
    fun `reject invalid recurrence fields`() {
        assertFailsWith<IllegalArgumentException> { rule.copy(intervalWeeks = 0) }
        assertFailsWith<IllegalArgumentException> { rule.copy(intervalWeeks = 121) }
        assertFailsWith<IllegalArgumentException> { rule.copy(weekdays = emptySet()) }
        assertFailsWith<IllegalArgumentException> { rule.copy(endDate = LocalDate(2026, 12, 29)) }
        assertFailsWith<IllegalArgumentException> { rule.copy(zoneId = "Invalid/Zone") }
    }

    @Test
    fun `seek across the partial first week and preserve numbering`() {
        val unbounded = rule.copy(endDate = null)

        assertEquals(
            listOf(
                ReminderOccurrence(2, Instant.parse("2027-01-01T09:00:00Z")),
                ReminderOccurrence(3, Instant.parse("2027-01-11T09:00:00Z")),
                ReminderOccurrence(4, Instant.parse("2027-01-13T09:00:00Z")),
            ),
            unbounded.occurrences(after = 1).take(3).toList(),
        )
        assertEquals(500_001, unbounded.occurrences(after = 500_000).first().number)
    }

    @Test
    fun `allow a single day inclusive range`() {
        val sameDay = rule.copy(endDate = rule.startDate)

        assertEquals(listOf(rule.startDate), sameDay.scheduledDates().toList())
        assertEquals(emptyList(), sameDay.occurrences(after = 1).toList())
    }
}
