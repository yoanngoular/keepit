package com.ygoular.keepit.reminder.presentation.model

import androidx.compose.runtime.saveable.listSaver
import com.ygoular.keepit.reminder.domain.MonthlyReminderRule
import com.ygoular.keepit.reminder.domain.ReminderNote
import com.ygoular.keepit.reminder.domain.ReminderRule
import com.ygoular.keepit.reminder.domain.WeeklyReminderRule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal data class NoteDraft(
    val id: String,
    val isNew: Boolean,
    val title: String,
    val description: String,
    val date: String,
    val time: String,
    val zoneId: String,
    val interval: String,
    val ordinal: Int,
    val weekday: DayOfWeek,
    val count: String,
    val cadence: RepeatCadence = RepeatCadence.MONTHLY,
    val intervalWeeks: String = "1",
    val weeklyDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY),
) {
    fun parseRule(): ReminderRule? = runCatching {
        val parsedDate = LocalDate.parse(date.trim())
        require(parsedDate.year in 1..9999)
        val parsedTime = LocalTime.parse(time.trim())
        val parsedCount = count.trim().toInt()
        when (cadence) {
            RepeatCadence.MONTHLY -> MonthlyReminderRule(
                startDate = parsedDate,
                time = parsedTime,
                zoneId = zoneId.trim(),
                intervalMonths = interval.trim().toInt(),
                ordinal = ordinal,
                weekday = weekday,
                count = parsedCount,
            )
            RepeatCadence.WEEKLY -> WeeklyReminderRule(
                startDate = parsedDate,
                time = parsedTime,
                zoneId = zoneId.trim(),
                intervalWeeks = intervalWeeks.trim().toInt(),
                weekdays = weeklyDays,
                count = parsedCount,
            )
        }
    }.getOrNull()

    fun toNote(rule: ReminderRule): ReminderNote = ReminderNote(
        id = id,
        title = title.trim(),
        description = description.trim(),
        rule = rule,
    )

    companion object {
        val saver = listSaver<NoteDraft?, String>(
            save = { draft ->
                draft?.run {
                    listOf(
                        id, title, description, date, time, zoneId, interval, ordinal.toString(), weekday.name,
                        count, isNew.toString(), cadence.name, intervalWeeks, weeklyDays.joinToString(",") { it.name },
                    )
                }.orEmpty()
            },
            restore = { values ->
                if (values.isEmpty()) null else NoteDraft(
                    id = values[0],
                    title = values[1],
                    description = values[2],
                    date = values[3],
                    time = values[4],
                    zoneId = values[5],
                    interval = values[6],
                    ordinal = values[7].toInt(),
                    weekday = DayOfWeek.valueOf(values[8]),
                    count = values[9],
                    isNew = values[10].toBooleanStrict(),
                    cadence = values.getOrNull(11)?.let(RepeatCadence::valueOf) ?: RepeatCadence.MONTHLY,
                    intervalWeeks = values.getOrNull(12) ?: "1",
                    weeklyDays = values.getOrNull(13)?.split(',')?.filter(String::isNotEmpty)
                        ?.map(DayOfWeek::valueOf)?.toSet() ?: setOf(DayOfWeek.MONDAY),
                )
            },
        )

        @OptIn(ExperimentalUuidApi::class)
        fun new(): NoteDraft {
            val zone = TimeZone.currentSystemDefault()
            return NoteDraft(
                id = Uuid.random().toString(),
                isNew = true,
                title = "",
                description = "",
                date = Clock.System.now().toLocalDateTime(zone).date.toString(),
                time = "09:00",
                zoneId = zone.id,
                interval = "2",
                ordinal = 2,
                weekday = DayOfWeek.TUESDAY,
                count = "6",
            )
        }

        fun fromNote(note: ReminderNote): NoteDraft {
            val rule = note.rule
            val draft = NoteDraft(
                id = note.id,
                isNew = false,
                title = note.title,
                description = note.description,
                date = rule.startDate.toString(),
                time = rule.time.toString(),
                zoneId = rule.zoneId,
                interval = "2",
                ordinal = 2,
                weekday = DayOfWeek.TUESDAY,
                count = rule.count.toString(),
            )
            return when (rule) {
                is MonthlyReminderRule -> draft.copy(
                    interval = rule.intervalMonths.toString(),
                    ordinal = rule.ordinal,
                    weekday = rule.weekday,
                )
                is WeeklyReminderRule -> draft.copy(
                    cadence = RepeatCadence.WEEKLY,
                    intervalWeeks = rule.intervalWeeks.toString(),
                    weeklyDays = rule.weekdays,
                )
            }
        }
    }
}
