package com.ygoular.keepit.reminder.domain

import kotlinx.datetime.DateTimeArithmeticException
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant

internal fun LocalDate.advanceOrNull(amount: Long, unit: DateTimeUnit.DateBased): LocalDate? =
    try {
        plus(amount, unit)
    } catch (_: DateTimeArithmeticException) {
        null
    }

internal fun Sequence<LocalDate>.toOccurrences(
    time: LocalTime,
    zone: TimeZone,
    after: Int,
): Sequence<ReminderOccurrence> = take(Int.MAX_VALUE - after).mapIndexed { index, date ->
    ReminderOccurrence(after + index + 1, LocalDateTime(date, time).toInstant(zone))
}

/** Resolve each calendar position from the original anchor so clamping never shifts later dates. */
internal fun LocalDate.anchoredDates(
    firstIndex: Long,
    interval: Int,
    unit: DateTimeUnit.DateBased,
    endDate: LocalDate?,
    dateInPeriod: (LocalDate) -> LocalDate = { it },
): Sequence<LocalDate> = sequence {
    var index = firstIndex
    while (true) {
        val period = advanceOrNull(index * interval, unit) ?: break
        val date = dateInPeriod(period)
        if (endDate != null && date > endDate) break
        yield(date)
        index++
    }
}
