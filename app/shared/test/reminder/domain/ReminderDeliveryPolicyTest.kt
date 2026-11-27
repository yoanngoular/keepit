package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
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
        count = 3,
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
}
