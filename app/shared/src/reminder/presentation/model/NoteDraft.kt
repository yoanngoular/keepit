package com.ygoular.keepit.reminder.presentation.model

import androidx.compose.runtime.saveable.listSaver
import com.ygoular.keepit.reminder.domain.MonthlyReminderRule
import com.ygoular.keepit.reminder.domain.OneTimeReminderRule
import com.ygoular.keepit.reminder.domain.ReminderNote
import com.ygoular.keepit.reminder.domain.ReminderRule
import com.ygoular.keepit.reminder.domain.WeeklyReminderRule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
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
    val endDate: LocalDate? = null,
    val cadence: RepeatCadence = RepeatCadence.ONCE,
    val intervalWeeks: String = "1",
    val weeklyDays: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY),
) {
    fun parseRule(): ReminderRule? = runCatching {
        val parsedDate = LocalDate.parse(date.trim())
        require(parsedDate.year in 1..9999)
        val parsedTime = LocalTime.parse(time.trim())
        when (cadence) {
            RepeatCadence.ONCE -> OneTimeReminderRule(parsedDate, parsedTime, zoneId.trim())
            RepeatCadence.MONTHLY -> MonthlyReminderRule(
                startDate = parsedDate,
                time = parsedTime,
                zoneId = zoneId.trim(),
                intervalMonths = interval.trim().toInt(),
                ordinal = ordinal,
                weekday = weekday,
                endDate = endDate,
            )
            RepeatCadence.WEEKLY -> WeeklyReminderRule(
                startDate = parsedDate,
                time = parsedTime,
                zoneId = zoneId.trim(),
                intervalWeeks = intervalWeeks.trim().toInt(),
                weekdays = weeklyDays,
                endDate = endDate,
            )
        }
    }.getOrNull()

    private fun restoreEndDate(value: String): LocalDate? {
        val legacyCount = value.toIntOrNull()
        if (legacyCount == null) return runCatching { LocalDate.parse(value) }.getOrNull()
        // Previous drafts stored an occurrence count at this position.
        if (legacyCount !in 1..100) return null
        val dates = when (val rule = parseRule()) {
            is MonthlyReminderRule -> rule.scheduledDates()
            is WeeklyReminderRule -> rule.scheduledDates()
            else -> return null
        }
        return dates.take(legacyCount).lastOrNull()
    }

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
                        endDate?.toString().orEmpty(), isNew.toString(), cadence.name,
                        intervalWeeks, weeklyDays.joinToString(",") { it.name },
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
                    isNew = values[10].toBooleanStrict(),
                    cadence = values.getOrNull(11)?.let(RepeatCadence::valueOf) ?: RepeatCadence.MONTHLY,
                    intervalWeeks = values.getOrNull(12) ?: "1",
                    weeklyDays = values.getOrNull(13)?.split(',')?.filter(String::isNotEmpty)
                        ?.map(DayOfWeek::valueOf)?.toSet() ?: setOf(DayOfWeek.MONDAY),
                ).let { it.copy(endDate = it.restoreEndDate(values[9])) }
            },
        )

        @OptIn(ExperimentalUuidApi::class)
        fun new(now: Instant = Clock.System.now(), zone: TimeZone = TimeZone.currentSystemDefault()): NoteDraft {
            val nextHour = generateSequence(now + 1.hours) { it + 1.hours }
                .map { it.toLocalDateTime(zone) }
                .map { LocalDateTime(it.date, LocalTime(it.hour, 0)) }
                .first { it.toInstant(zone) > now }
            return NoteDraft(
                id = Uuid.random().toString(),
                isNew = true,
                title = "",
                description = "",
                date = nextHour.date.toString(),
                time = nextHour.time.toString(),
                zoneId = zone.id,
                interval = "2",
                ordinal = 2,
                weekday = DayOfWeek.TUESDAY,
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
            )
            return when (rule) {
                is OneTimeReminderRule -> draft
                is MonthlyReminderRule -> draft.copy(
                    cadence = RepeatCadence.MONTHLY,
                    interval = rule.intervalMonths.toString(),
                    ordinal = rule.ordinal,
                    weekday = rule.weekday,
                    endDate = rule.endDate,
                )
                is WeeklyReminderRule -> draft.copy(
                    cadence = RepeatCadence.WEEKLY,
                    intervalWeeks = rule.intervalWeeks.toString(),
                    weeklyDays = rule.weekdays,
                    endDate = rule.endDate,
                )
            }
        }
    }
}
