package com.ygoular.keepit.reminder.presentation.format

import androidx.compose.runtime.Composable
import com.ygoular.keepit.reminder.domain.ReminderRule
import com.ygoular.keepit.reminder.domain.ReminderOccurrence
import com.ygoular.keepit.resources.*
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun occurrenceLabel(occurrence: ReminderOccurrence, rule: ReminderRule): String {
    val local = occurrence.at.toLocalDateTime(TimeZone.of(rule.zoneId))
    return stringResource(
        Res.string.reminder_occurrence_date,
        weekdayLabel(local.date.dayOfWeek), local.date.day, monthLabel(local.date.month.ordinal),
        local.date.year, formatTime(local.time),
    )
}

@Composable
internal fun reminderDateLabel(date: LocalDate): String = stringResource(
    Res.string.reminder_date_value, date.day, monthLabel(date.month.ordinal), date.year,
)

internal fun formatTime(time: LocalTime): String =
    time.hour.toString().padStart(2, '0') + ":" + time.minute.toString().padStart(2, '0')

@Composable
internal fun weekdayLabel(weekday: DayOfWeek): String = stringResource(weekdayLabels[weekday.ordinal])

@Composable
private fun monthLabel(index: Int): String = stringResource(monthLabels[index])

@Composable
internal fun ordinalLabel(ordinal: Int): String = stringResource(ordinalLabels[ordinal - 1])

private val weekdayLabels = listOf(
    Res.string.reminder_monday, Res.string.reminder_tuesday, Res.string.reminder_wednesday,
    Res.string.reminder_thursday, Res.string.reminder_friday, Res.string.reminder_saturday, Res.string.reminder_sunday,
)

private val monthLabels = listOf(
    Res.string.reminder_jan, Res.string.reminder_feb, Res.string.reminder_mar, Res.string.reminder_apr,
    Res.string.reminder_may, Res.string.reminder_jun, Res.string.reminder_jul, Res.string.reminder_aug,
    Res.string.reminder_sep, Res.string.reminder_oct, Res.string.reminder_nov, Res.string.reminder_dec,
)

private val ordinalLabels = listOf(
    Res.string.reminder_first, Res.string.reminder_second, Res.string.reminder_third,
    Res.string.reminder_fourth, Res.string.reminder_fifth,
)
