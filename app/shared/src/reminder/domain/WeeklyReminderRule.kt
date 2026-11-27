package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber

data class WeeklyReminderRule(
    override val startDate: LocalDate,
    override val time: LocalTime,
    override val zoneId: String,
    val intervalWeeks: Int,
    val weekdays: Set<DayOfWeek>,
    val endDate: LocalDate?,
) : ReminderRule {
    private val zone: TimeZone

    init {
        require(intervalWeeks in 1..120) { "Week interval must be between 1 and 120." }
        require(weekdays.isNotEmpty()) { "At least one weekday is required." }
        require(endDate == null || endDate >= startDate) { "End date must not be before the start date." }
        zone = TimeZone.of(zoneId)
    }

    override fun occurrences(after: Int): Sequence<ReminderOccurrence> {
        require(after >= 0) { "Occurrence number must not be negative." }
        if (after == Int.MAX_VALUE) return emptySequence()
        return datesAfter(after).toOccurrences(time, zone, after)
    }

    internal fun scheduledDates(): Sequence<LocalDate> = datesAfter(0)

    /** The ISO week containing [startDate] anchors the series. A partial first week keeps that anchor. */
    private fun datesAfter(after: Int): Sequence<LocalDate> = sequence {
        val startWeekday = startDate.dayOfWeek.isoDayNumber
        val weekdayOffsets = weekdays.map { it.isoDayNumber - startWeekday }.sorted()
        val omittedDays = weekdayOffsets.count { it < 0 }
        val position = after.toLong() + omittedDays
        val weekLength = intervalWeeks * 7
        var weekOffset = (position / weekdayOffsets.size) * weekLength
        var dayIndex = (position % weekdayOffsets.size).toInt()

        while (true) {
            val daysSinceStart = weekOffset + weekdayOffsets[dayIndex]
            val date = startDate.advanceOrNull(daysSinceStart, DateTimeUnit.DAY) ?: break
            if (endDate != null && date > endDate) break
            yield(date)
            dayIndex++
            if (dayIndex == weekdayOffsets.size) {
                dayIndex = 0
                weekOffset += weekLength
            }
        }
    }
}
