package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class OneTimeReminderRuleTest {
    private val rule = OneTimeReminderRule(
        startDate = LocalDate(2027, 1, 7),
        time = LocalTime(14, 35),
        zoneId = "UTC",
    )

    @Test
    fun `resolve exactly one occurrence on the configured date`() {
        assertEquals(1, rule.count)
        assertEquals(
            listOf(ReminderOccurrence(1, Instant.parse("2027-01-07T14:35:00Z"))),
            rule.occurrences(),
        )
    }

    @Test
    fun `use the rule time zone to resolve the local wall time`() {
        assertEquals(
            listOf(ReminderOccurrence(1, Instant.parse("2027-01-07T09:05:00Z"))),
            rule.copy(zoneId = "+05:30").occurrences(),
        )
        assertFailsWith<IllegalArgumentException> { rule.copy(zoneId = "Invalid/Zone") }
    }

    @Test
    fun `finish delivery after the only occurrence is consumed`() {
        val due = Instant.parse("2027-01-07T14:35:00Z")
        assertEquals(0, ReminderDeliveryPolicy.initialConsumed(rule, Instant.parse("2027-01-07T14:34:59Z")))
        assertEquals(ReminderOccurrence(1, due), ReminderDeliveryPolicy.next(rule, consumed = 0, now = due))
        assertEquals(1, ReminderDeliveryPolicy.initialConsumed(rule, due))
        assertNull(ReminderDeliveryPolicy.next(rule, consumed = 1, now = due))
    }

    @Test
    fun `new reminder must be strictly in the future`() {
        assertFalse(rule.isPastOneTimeChange(previous = null, now = Instant.parse("2027-01-07T14:34:59Z")))
        assertTrue(rule.isPastOneTimeChange(previous = null, now = Instant.parse("2027-01-07T14:35:00Z")))
        assertTrue(rule.isPastOneTimeChange(previous = null, now = Instant.parse("2027-01-07T14:35:01Z")))
    }

    @Test
    fun `editing a note with an unchanged past reminder is allowed`() {
        val now = Instant.parse("2027-01-07T14:36:00Z")
        assertFalse(rule.isPastOneTimeChange(previous = rule.copy(), now = now))
        assertTrue(rule.isPastOneTimeChange(previous = rule.copy(time = LocalTime(14, 37)), now = now))
        assertFalse(rule.copy(time = LocalTime(14, 37)).isPastOneTimeChange(previous = rule, now = now))
    }
}
