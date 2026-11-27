package com.ygoular.keepit.reminder.domain

import kotlin.time.Instant

object ReminderDeliveryPolicy {
    /** Occurrences that have already passed are skipped when a series is first saved. */
    fun initialConsumed(rule: ReminderRule, now: Instant): Int =
        if (rule.hasDirectCursor()) {
            rule.firstMatchingIndex { it > now }
        } else {
            rule.occurrences().takeWhile { it.at <= now }.count()
        }

    fun upcoming(rule: ReminderRule, now: Instant, limit: Int): List<ReminderOccurrence> {
        require(limit >= 0) { "Occurrence limit must not be negative." }
        if (limit == 0) return emptyList()
        val occurrences = if (rule.hasDirectCursor()) {
            rule.occurrences(after = rule.firstMatchingIndex { it >= now })
        } else {
            rule.occurrences().dropWhile { it.at < now }
        }
        return occurrences.take(limit).toList()
    }

    /** Coalesce missed occurrences into the latest due one, then continue the same series. */
    fun next(rule: ReminderRule, consumed: Int, now: Instant): ReminderOccurrence? {
        var latestDue: ReminderOccurrence? = null
        for (occurrence in rule.occurrences(after = consumed)) {
            if (occurrence.at > now) return latestDue ?: occurrence
            latestDue = occurrence
        }
        return latestDue
    }

    fun onAlarm(
        rule: ReminderRule,
        consumed: Int,
        now: Instant,
        revision: String,
        alarmRevision: String?,
        alarmNumber: Int,
    ): AlarmDecision {
        if (revision != alarmRevision) return AlarmDecision.Ignore
        val expected = next(rule, consumed, now) ?: return AlarmDecision.Ignore
        return if (expected.number == alarmNumber && expected.at <= now) {
            AlarmDecision.Deliver(expected)
        } else {
            AlarmDecision.Rearm(expected)
        }
    }

    sealed interface AlarmDecision {
        data object Ignore : AlarmDecision
        data class Rearm(val occurrence: ReminderOccurrence) : AlarmDecision
        data class Deliver(val occurrence: ReminderOccurrence) : AlarmDecision
    }
}

private fun ReminderRule.hasDirectCursor(): Boolean =
    this is WeeklyReminderRule || (this is MonthlyReminderRule && ordinal < 5)

/** A missing occurrence is the upper boundary for a finite series. */
private fun ReminderRule.firstMatchingIndex(matches: (Instant) -> Boolean): Int {
    fun atBoundary(index: Int): Boolean =
        occurrences(after = index).firstOrNull()?.let { matches(it.at) } != false

    if (atBoundary(0)) return 0

    var lower = 0
    var upper = 1
    while (!atBoundary(upper)) {
        lower = upper
        upper = (upper.toLong() * 2).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    while (upper - lower > 1) {
        val middle = lower + (upper - lower) / 2
        if (atBoundary(middle)) upper = middle else lower = middle
    }
    return upper
}
