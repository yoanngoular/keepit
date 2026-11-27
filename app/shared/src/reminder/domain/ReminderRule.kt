package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

sealed interface ReminderRule {
    val startDate: LocalDate
    val time: LocalTime
    val zoneId: String

    /** Occurrences with a number greater than [after], in chronological order. */
    fun occurrences(after: Int = 0): Sequence<ReminderOccurrence>
}
