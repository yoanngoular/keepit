package com.ygoular.keepit.reminder

import androidx.compose.runtime.saveable.listSaver
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
) {
    fun parseRule(): MonthlyReminderRule? = runCatching {
        val parsedDate = LocalDate.parse(date.trim())
        require(parsedDate.year in 1..9999)
        MonthlyReminderRule(
            startDate = parsedDate,
            time = LocalTime.parse(time.trim()),
            zoneId = zoneId.trim(),
            intervalMonths = interval.trim().toInt(),
            ordinal = ordinal,
            weekday = weekday,
            count = count.trim().toInt(),
        )
    }.getOrNull()

    fun toNote(rule: MonthlyReminderRule): ReminderNote = ReminderNote(
        id = id,
        title = title.trim(),
        description = description.trim(),
        rule = rule,
    )

    companion object {
        val saver = listSaver<NoteDraft?, String>(
            save = { draft ->
                draft?.run {
                    listOf(id, title, description, date, time, zoneId, interval, ordinal.toString(), weekday.name, count, isNew.toString())
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

        fun fromNote(note: ReminderNote): NoteDraft = with(note.rule) {
            NoteDraft(
                id = note.id,
                isNew = false,
                title = note.title,
                description = note.description,
                date = startDate.toString(),
                time = time.toString(),
                zoneId = zoneId,
                interval = intervalMonths.toString(),
                ordinal = ordinal,
                weekday = weekday,
                count = count.toString(),
            )
        }
    }
}
