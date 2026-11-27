package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.daysUntil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ReminderDeliveryPolicyTest {
    private val rule = MonthlyReminderRule(
        startDate = LocalDate(2026, 1, 1),
        time = LocalTime(9, 0),
        zoneId = "UTC",
        intervalMonths = 2,
        ordinal = 2,
        weekday = DayOfWeek.TUESDAY,
        endDate = LocalDate(2026, 5, 12),
    )
    private val first = ReminderOccurrence(1, Instant.parse("2026-01-13T09:00:00Z"))
    private val second = ReminderOccurrence(2, Instant.parse("2026-03-10T09:00:00Z"))
    private val third = ReminderOccurrence(3, Instant.parse("2026-05-12T09:00:00Z"))
    private val beforeFirst = Instant.parse("2026-01-13T08:59:59Z")
    private val afterSecond = Instant.parse("2026-03-10T09:00:01Z")

    @Test
    fun `new rule skips occurrences at or before save time`() {
        assertEquals(0, ReminderDeliveryPolicy.initialConsumed(rule, beforeFirst))
        assertEquals(1, ReminderDeliveryPolicy.initialConsumed(rule, first.at))
        assertEquals(2, ReminderDeliveryPolicy.initialConsumed(rule, afterSecond))
    }

    @Test
    fun `select latest overdue occurrence and then continue the series`() {
        assertEquals(first, ReminderDeliveryPolicy.next(rule, consumed = 0, now = beforeFirst))
        assertEquals(second, ReminderDeliveryPolicy.next(rule, consumed = 0, now = afterSecond))
        assertEquals(third, ReminderDeliveryPolicy.next(rule, consumed = 2, now = afterSecond))
        assertNull(ReminderDeliveryPolicy.next(rule, consumed = 3, now = third.at))
    }

    @Test
    fun `ignore an alarm from a replaced rule or a finished series`() {
        assertEquals(
            ReminderDeliveryPolicy.AlarmDecision.Ignore,
            ReminderDeliveryPolicy.onAlarm(rule, 0, first.at, "current", "old", 1),
        )
        assertEquals(
            ReminderDeliveryPolicy.AlarmDecision.Ignore,
            ReminderDeliveryPolicy.onAlarm(rule, 3, third.at, "current", "current", 3),
        )
    }

    @Test
    fun `rearm when an alarm arrives early or a later occurrence is overdue`() {
        assertEquals(
            ReminderDeliveryPolicy.AlarmDecision.Rearm(first),
            ReminderDeliveryPolicy.onAlarm(rule, 0, beforeFirst, "current", "current", 1),
        )
        assertEquals(
            ReminderDeliveryPolicy.AlarmDecision.Rearm(second),
            ReminderDeliveryPolicy.onAlarm(rule, 0, afterSecond, "current", "current", 1),
        )
        assertEquals(
            ReminderDeliveryPolicy.AlarmDecision.Rearm(second),
            ReminderDeliveryPolicy.onAlarm(rule, 1, first.at, "current", "current", 1),
        )
    }

    @Test
    fun `deliver the expected occurrence once it is due`() {
        assertEquals(
            ReminderDeliveryPolicy.AlarmDecision.Deliver(first),
            ReminderDeliveryPolicy.onAlarm(rule, 0, first.at, "current", "current", 1),
        )
    }

    @Test
    fun `open ended rule returns without reading past the first future date`() {
        val unbounded = rule.copy(endDate = null)

        assertEquals(2, ReminderDeliveryPolicy.initialConsumed(unbounded, afterSecond))
        assertEquals(second, ReminderDeliveryPolicy.next(unbounded, consumed = 0, now = afterSecond))
        assertEquals(third, ReminderDeliveryPolicy.next(unbounded, consumed = 2, now = afterSecond))
    }

    @Test
    fun `upcoming includes an exact match and never exceeds the requested limit`() {
        assertEquals(listOf(first, second), ReminderDeliveryPolicy.upcoming(rule, first.at, limit = 2))
        assertEquals(emptyList(), ReminderDeliveryPolicy.upcoming(rule, first.at, limit = 0))
        assertEquals(listOf(third), ReminderDeliveryPolicy.upcoming(rule, afterSecond, limit = 2))
        assertEquals(
            emptyList(),
            ReminderDeliveryPolicy.upcoming(rule, Instant.parse("2026-05-12T09:00:01Z"), limit = 2),
        )
    }

    @Test
    fun `seek old monthly and weekly anchors without changing occurrence numbers`() {
        val origin = LocalDate(1, 1, 1)
        val requestedDate = LocalDate(2026, 8, 3)
        val now = Instant.parse("2026-08-03T09:00:00Z")
        val monthly = rule.copy(
            startDate = origin, intervalMonths = 1, weekday = DayOfWeek.MONDAY, ordinal = 1, endDate = null,
        )
        val weekly = WeeklyReminderRule(
            origin, LocalTime(9, 0), "UTC", 1, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), endDate = null,
        )
        val elapsedMonths = (requestedDate.year - origin.year) * 12 + 7
        val elapsedWeeks = origin.daysUntil(requestedDate) / 7

        assertEquals(
            listOf(ReminderOccurrence(elapsedMonths + 1, now)),
            ReminderDeliveryPolicy.upcoming(monthly, now, limit = 1),
        )
        assertEquals(
            listOf(
                ReminderOccurrence(elapsedWeeks * 2 + 1, now),
                ReminderOccurrence(elapsedWeeks * 2 + 2, Instant.parse("2026-08-05T09:00:00Z")),
            ),
            ReminderDeliveryPolicy.upcoming(weekly, now, limit = 2),
        )
        assertEquals(elapsedWeeks * 2 + 1, ReminderDeliveryPolicy.initialConsumed(weekly, now))
    }

    @Test
    fun `upcoming can skip missing fifth weekdays`() {
        val fifth = rule.copy(intervalMonths = 1, ordinal = 5, endDate = null)

        assertEquals(
            listOf(
                ReminderOccurrence(2, Instant.parse("2026-06-30T09:00:00Z")),
                ReminderOccurrence(3, Instant.parse("2026-09-29T09:00:00Z")),
            ),
            ReminderDeliveryPolicy.upcoming(fifth, Instant.parse("2026-05-01T00:00:00Z"), limit = 2),
        )
    }
}
