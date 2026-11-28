package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.yearMonth

data class MonthlyReminderRule(
    override val startDate: LocalDate,
    override val time: LocalTime,
    override val zoneId: String,
    val intervalMonths: Int,
    val ordinal: Int,
    val weekday: DayOfWeek,
    val endDate: LocalDate?,
) : ReminderRule {
    private val zone: TimeZone

    init {
        require(intervalMonths in 1..120) { "Month interval must be between 1 and 120." }
        require(ordinal == LAST || ordinal in 1..5) { "Weekday ordinal must be between 1 and 5 or last." }
        require(endDate == null || endDate >= startDate) { "End date must not be before the start date." }
        zone = TimeZone.of(zoneId)
    }

    override fun occurrences(after: Int): Sequence<ReminderOccurrence> {
        require(after >= 0) { "Occurrence number must not be negative." }
        if (after == Int.MAX_VALUE) return emptySequence()
        val dates = if (ordinal == 5) {
            scheduledDates().drop(after)
        } else {
            // The first four ordinals and last weekday exist in every month.
            val startMonth = LocalDate(startDate.year, startDate.month, 1)
            val firstMonthIndex = if (checkNotNull(dateIn(startMonth)) < startDate) 1 else 0
            datesFromMonth(startMonth, after.toLong() + firstMonthIndex)
        }
        return dates.toOccurrences(time, zone, after)
    }

    internal fun scheduledDates(): Sequence<LocalDate> =
        datesFromMonth(LocalDate(startDate.year, startDate.month, 1), 0)

    private fun dateIn(month: LocalDate): LocalDate? {
        if (ordinal == LAST) {
            val lastDay = month.yearMonth.lastDay
            val offset = (lastDay.dayOfWeek.isoDayNumber - weekday.isoDayNumber + 7) % 7
            return lastDay.advanceOrNull(-offset.toLong(), DateTimeUnit.DAY)
        }
        val firstMatch = (weekday.isoDayNumber - month.dayOfWeek.isoDayNumber + 7) % 7
        return month.advanceOrNull((firstMatch + (ordinal - 1) * 7).toLong(), DateTimeUnit.DAY)
            ?.takeIf { it.month == month.month }
    }

    private fun datesFromMonth(startMonth: LocalDate, monthIndex: Long): Sequence<LocalDate> = sequence {
        var month = startMonth.advanceOrNull(monthIndex * intervalMonths, DateTimeUnit.MONTH) ?: return@sequence
        var foundMatchingMonth = false
        while (endDate == null || month <= endDate) {
            val candidate = dateIn(month)
            if (candidate != null) {
                foundMatchingMonth = true
                if (endDate != null && candidate > endDate) break
                if (candidate >= startDate) yield(candidate)
            }

            month = month.advanceOrNull(intervalMonths.toLong(), DateTimeUnit.MONTH) ?: break
            // The arrangement of weekdays repeats every 400 Gregorian years.
            if (!foundMatchingMonth && month.month == startMonth.month &&
                (month.year - startMonth.year) % 400 == 0
            ) break
        }
    }

    companion object {
        const val LAST = -1
    }
}
