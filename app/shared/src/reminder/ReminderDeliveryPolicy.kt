package com.ygoular.keepit.reminder

import kotlin.time.Instant

object ReminderDeliveryPolicy {
    /** Occurrences that have already passed are skipped when a series is first saved. */
    fun initialConsumed(rule: MonthlyReminderRule, now: Instant): Int =
        rule.occurrences().lastOrNull { it.at <= now }?.number ?: 0

    /** Coalesce missed occurrences into the latest due one, then continue the same series. */
    fun next(rule: MonthlyReminderRule, consumed: Int, now: Instant): ReminderOccurrence? {
        val pending = rule.occurrences().filter { it.number > consumed }
        return pending.lastOrNull { it.at <= now } ?: pending.firstOrNull()
    }

    fun onAlarm(
        rule: MonthlyReminderRule,
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
