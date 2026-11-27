package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class MonthlyReminderRuleTest {
    private val secondTuesday = MonthlyReminderRule(
        startDate = LocalDate(2026, 1, 1),
        time = LocalTime(9, 0),
        zoneId = "UTC",
        intervalMonths = 2,
        ordinal = 2,
        weekday = DayOfWeek.TUESDAY,
        count = 6,
    )

    @Test
    fun `schedule six second Tuesdays at two-month intervals`() {
        val expected = listOf(
            "2026-01-13T09:00:00Z",
            "2026-03-10T09:00:00Z",
            "2026-05-12T09:00:00Z",
            "2026-07-14T09:00:00Z",
            "2026-09-08T09:00:00Z",
            "2026-11-10T09:00:00Z",
        ).mapIndexed { index, time -> ReminderOccurrence(index + 1, Instant.parse(time)) }

        assertEquals(expected, secondTuesday.occurrences())
    }

    @Test
    fun `include the start date when it matches`() {
        val rule = secondTuesday.copy(startDate = LocalDate(2026, 1, 13), count = 1)

        assertEquals(listOf(ReminderOccurrence(1, Instant.parse("2026-01-13T09:00:00Z"))), rule.occurrences())
    }

    @Test
    fun `skip dates before start without moving the series anchor`() {
        val rule = secondTuesday.copy(startDate = LocalDate(2026, 1, 14), count = 1)

        assertEquals(listOf(ReminderOccurrence(1, Instant.parse("2026-03-10T09:00:00Z"))), rule.occurrences())
    }

    @Test
    fun `skip months without a fifth weekday and keep consecutive occurrence numbers`() {
        val rule = secondTuesday.copy(
            startDate = LocalDate(2026, 4, 1),
            intervalMonths = 1,
            ordinal = 5,
            count = 2,
        )

        assertEquals(
            listOf(
                ReminderOccurrence(1, Instant.parse("2026-06-30T09:00:00Z")),
                ReminderOccurrence(2, Instant.parse("2026-09-29T09:00:00Z")),
            ),
            rule.occurrences(),
        )
    }

    @Test
    fun `stop when the month pattern can never contain the requested fifth weekday`() {
        val rule = secondTuesday.copy(
            startDate = LocalDate(2027, 2, 1),
            intervalMonths = 120,
            ordinal = 5,
            count = 1,
        )

        assertEquals(emptyList(), rule.occurrences())
    }

    @Test
    fun `resolve wall time in the rule timezone`() {
        val rule = secondTuesday.copy(zoneId = "+02:00", count = 1)

        assertEquals(listOf(ReminderOccurrence(1, Instant.parse("2026-01-13T07:00:00Z"))), rule.occurrences())
    }

    @Test
    fun `reject invalid recurrence fields`() {
        assertFailsWith<IllegalArgumentException> { secondTuesday.copy(intervalMonths = 0) }
        assertFailsWith<IllegalArgumentException> { secondTuesday.copy(intervalMonths = 121) }
        assertFailsWith<IllegalArgumentException> { secondTuesday.copy(ordinal = 0) }
        assertFailsWith<IllegalArgumentException> { secondTuesday.copy(ordinal = 6) }
        assertFailsWith<IllegalArgumentException> { secondTuesday.copy(count = 0) }
        assertFailsWith<IllegalArgumentException> { secondTuesday.copy(count = 101) }
        assertFailsWith<IllegalArgumentException> { secondTuesday.copy(zoneId = "Invalid/Zone") }
    }
}
