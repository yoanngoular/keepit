package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.yearMonth

data class MonthlyDayReminderRule(
    override val startDate: LocalDate,
    override val time: LocalTime,
    override val zoneId: String,
    val intervalMonths: Int,
    val dayOfMonth: Int,
    val endDate: LocalDate?,
) : ReminderRule {
    private val zone: TimeZone

    init {
        require(intervalMonths in 1..120) { "Month interval must be between 1 and 120." }
        require(dayOfMonth in 1..31) { "Day of month must be between 1 and 31." }
        require(endDate == null || endDate >= startDate) { "End date must not be before the start date." }
        zone = TimeZone.of(zoneId)
    }

    override fun occurrences(after: Int): Sequence<ReminderOccurrence> {
        require(after >= 0) { "Occurrence number must not be negative." }
        val startMonth = startDate.yearMonth.firstDay
        val firstMonthIndex = if (dateIn(startMonth) < startDate) 1 else 0
        return startMonth.anchoredDates(after.toLong() + firstMonthIndex, intervalMonths, DateTimeUnit.MONTH, endDate, ::dateIn)
            .toOccurrences(time, zone, after)
    }

    private fun dateIn(month: LocalDate): LocalDate =
        LocalDate(month.year, month.month, minOf(dayOfMonth, month.yearMonth.numberOfDays))
}
