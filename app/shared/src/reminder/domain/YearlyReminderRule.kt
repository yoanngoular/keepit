package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone

data class YearlyReminderRule(
    override val startDate: LocalDate,
    override val time: LocalTime,
    override val zoneId: String,
    val intervalYears: Int,
    val endDate: LocalDate?,
) : ReminderRule {
    private val zone: TimeZone

    init {
        require(intervalYears in 1..120) { "Year interval must be between 1 and 120." }
        require(endDate == null || endDate >= startDate) { "End date must not be before the start date." }
        zone = TimeZone.of(zoneId)
    }

    override fun occurrences(after: Int): Sequence<ReminderOccurrence> {
        require(after >= 0) { "Occurrence number must not be negative." }
        return startDate.anchoredDates(after.toLong(), intervalYears, DateTimeUnit.YEAR, endDate)
            .toOccurrences(time, zone, after)
    }
}
