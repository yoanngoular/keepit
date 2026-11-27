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
