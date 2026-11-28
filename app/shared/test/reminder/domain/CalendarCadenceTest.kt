package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class CalendarCadenceTest {
    private val time = LocalTime(9, 0)
    private val zone = "UTC"

    @Test
    fun `daily series skips calendar days and includes the end date`() {
        val rule = DailyReminderRule(date("2026-12-29"), time, zone, 2, date("2027-01-02"))

        assertEquals(listOf("2026-12-29", "2026-12-31", "2027-01-02"), rule.occurrenceDates())
        assertEquals(ReminderOccurrence(3, Instant.parse("2027-01-02T09:00:00Z")), rule.occurrences(after = 2).single())
        assertEquals(emptyList(), rule.occurrenceDates(after = 3))
    }

    @Test
    fun `yearly leap-day series clamps each target year from the original date`() {
        val rule = YearlyReminderRule(date("2024-02-29"), time, zone, 2, date("2028-02-29"))

        assertEquals(listOf("2024-02-29", "2026-02-28", "2028-02-29"), rule.occurrenceDates())
        assertEquals(ReminderOccurrence(3, Instant.parse("2028-02-29T09:00:00Z")), rule.occurrences(after = 2).single())
    }

    @Test
    fun `monthly dates clamp in short months without losing the original day`() {
        val rule = MonthlyDayReminderRule(date("2027-01-31"), time, zone, 1, dayOfMonth = 31, endDate = date("2027-04-30"))

        assertEquals(listOf("2027-01-31", "2027-02-28", "2027-03-31", "2027-04-30"), rule.occurrenceDates())
        assertEquals(listOf("2027-03-31", "2027-04-30"), rule.occurrenceDates(after = 2))
        assertEquals(listOf(3, 4), rule.occurrences(after = 2).map { it.number }.toList())
    }

    @Test
    fun `partial first month skipped without moving the monthly anchor`() {
        val rule = MonthlyDayReminderRule(date("2027-01-16"), time, zone, 2, dayOfMonth = 15, endDate = date("2027-05-14"))

        assertEquals(listOf("2027-03-15"), rule.occurrenceDates())
    }

    @Test
    fun `last weekday exists in short months while fifth weekday can skip them`() {
        val rule = MonthlyReminderRule(
            date("2027-01-30"), time, zone, 1, MonthlyReminderRule.LAST, DayOfWeek.FRIDAY, date("2027-04-30"),
        )

        assertEquals(listOf("2027-02-26", "2027-03-26", "2027-04-30"), rule.occurrenceDates())
        assertEquals(listOf("2027-03-26", "2027-04-30"), rule.occurrenceDates(after = 1))
        assertEquals(listOf("2027-04-30"), rule.copy(ordinal = 5).occurrenceDates())
        assertFailsWith<IllegalArgumentException> { rule.copy(ordinal = -2) }
    }

    @Test
    fun `new cadences seek bounded previews in long-running series`() {
        val start = date("2000-01-01")
        val now = Instant.parse("2027-02-01T09:00:00Z")
        val daily = DailyReminderRule(start, time, zone, 1, null)
        val monthly = MonthlyDayReminderRule(start, time, zone, 1, 31, null)
        val yearly = YearlyReminderRule(start, time, zone, 1, null)

        assertEquals(now, ReminderDeliveryPolicy.upcoming(daily, now, limit = 1).single().at)
        assertEquals(
            ReminderOccurrence(326, Instant.parse("2027-02-28T09:00:00Z")),
            ReminderDeliveryPolicy.upcoming(monthly, now, limit = 1).single(),
        )
        assertEquals(
            ReminderOccurrence(29, Instant.parse("2028-01-01T09:00:00Z")),
            ReminderDeliveryPolicy.upcoming(yearly, now, limit = 1).single(),
        )
    }

    @Test
    fun `validate intervals and the end date consistently`() {
        val start = date("2027-01-01")
        val before = date("2026-12-31")

        assertFailsWith<IllegalArgumentException> { DailyReminderRule(start, time, zone, 0, null) }
        assertFailsWith<IllegalArgumentException> { DailyReminderRule(start, time, zone, 1, before) }
        assertFailsWith<IllegalArgumentException> { YearlyReminderRule(start, time, zone, 121, null) }
        assertFailsWith<IllegalArgumentException> { YearlyReminderRule(start, time, zone, 1, before) }
        assertFailsWith<IllegalArgumentException> { MonthlyDayReminderRule(start, time, zone, 0, 1, null) }
        assertFailsWith<IllegalArgumentException> { MonthlyDayReminderRule(start, time, zone, 1, 32, null) }
        assertFailsWith<IllegalArgumentException> { MonthlyDayReminderRule(start, time, zone, 1, 1, before) }
    }

    private fun date(value: String): LocalDate = LocalDate.parse(value)

    private fun ReminderRule.occurrenceDates(after: Int = 0): List<String> =
        occurrences(after).map { it.at.toString().substringBefore('T') }.toList()
}
