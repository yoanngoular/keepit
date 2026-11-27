package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

data class WeeklyReminderRule(
    override val startDate: LocalDate,
    override val time: LocalTime,
    override val zoneId: String,
    val intervalWeeks: Int,
    val weekdays: Set<DayOfWeek>,
    override val count: Int,
) : ReminderRule {
    private val zone: TimeZone

    init {
        require(intervalWeeks in 1..120) { "Week interval must be between 1 and 120." }
        require(weekdays.isNotEmpty()) { "At least one weekday is required." }
        require(count in 1..100) { "Occurrence count must be between 1 and 100." }
        zone = TimeZone.of(zoneId)
    }

    /** The ISO week containing [startDate] anchors the series. A partial first week keeps that anchor. */
    override fun occurrences(): List<ReminderOccurrence> = buildList {
        val startWeekday = startDate.dayOfWeek.isoDayNumber
        val weekdayOffsets = weekdays.map { it.isoDayNumber - startWeekday }.sorted()
        var weekOffset = 0

        while (size < count) {
            for (offset in weekdayOffsets) {
                val daysSinceStart = weekOffset + offset
                if (daysSinceStart < 0) continue

                val date = startDate.plus(daysSinceStart, DateTimeUnit.DAY)
                val instant = LocalDateTime(date, time).toInstant(zone)
                add(ReminderOccurrence(size + 1, instant))
                if (size == count) return@buildList
            }
            weekOffset += intervalWeeks * 7
        }
    }
}
