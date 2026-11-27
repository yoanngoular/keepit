package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

data class OneTimeReminderRule(
    override val startDate: LocalDate,
    override val time: LocalTime,
    override val zoneId: String,
) : ReminderRule {
    private val zone = TimeZone.of(zoneId)

    override fun occurrences(after: Int): Sequence<ReminderOccurrence> {
        require(after >= 0) { "Occurrence number must not be negative." }
        if (after >= 1) return emptySequence()
        return sequenceOf(ReminderOccurrence(1, LocalDateTime(startDate, time).toInstant(zone)))
    }
}

/** An unchanged reminder does not block editing the note after its scheduled time. */
fun ReminderRule.isPastOneTimeChange(previous: ReminderRule?, now: Instant): Boolean =
    this is OneTimeReminderRule && this != previous && occurrences().single().at <= now
