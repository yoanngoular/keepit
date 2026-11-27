package com.ygoular.keepit.reminder

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

data class MonthlyReminderRule(
    val startDate: LocalDate,
    val time: LocalTime,
    val zoneId: String,
    val intervalMonths: Int,
    val ordinal: Int,
    val weekday: DayOfWeek,
    val count: Int,
) {
    private val zone: TimeZone

    init {
        require(intervalMonths in 1..120) { "Month interval must be between 1 and 120." }
        require(ordinal in 1..5) { "Weekday ordinal must be between 1 and 5." }
        require(count in 1..100) { "Occurrence count must be between 1 and 100." }
        zone = TimeZone.of(zoneId)
    }

    /**
     * Calculates the series from its start month in the rule's timezone.
     *
     * A missing fifth weekday is skipped. An impossible month pattern returns an empty list.
     * During a clock change, a nonexistent local time moves forward by the gap and
     * a repeated local time uses its earlier instant.
     */
    fun occurrences(): List<ReminderOccurrence> = buildList {
        val startMonth = LocalDate(startDate.year, startDate.month, 1)
        var month = startMonth
        var foundMatchingMonth = false

        while (size < count) {
            val firstMatch = (weekday.isoDayNumber - month.dayOfWeek.isoDayNumber + 7) % 7
            val candidate = month.plus(firstMatch + (ordinal - 1) * 7, DateTimeUnit.DAY)

            if (candidate.month == month.month) {
                foundMatchingMonth = true
                if (candidate >= startDate) {
                    val instant = LocalDateTime(candidate, time).toInstant(zone)
                    add(ReminderOccurrence(size + 1, instant))
                }
            }

            if (size == count) break
            month = month.plus(intervalMonths, DateTimeUnit.MONTH)

            // The arrangement of weekdays repeats every 400 Gregorian years.
            if (!foundMatchingMonth && month.month == startMonth.month &&
                (month.year - startMonth.year) % 400 == 0
            ) break
        }
    }
}
