package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

sealed interface ReminderRule {
    val startDate: LocalDate
    val time: LocalTime
    val zoneId: String
    val count: Int

    fun occurrences(): List<ReminderOccurrence>
}
