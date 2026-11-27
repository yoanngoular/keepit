package com.ygoular.keepit.reminder.storage

import androidx.sqlite.SQLiteStatement
import com.ygoular.keepit.reminder.domain.MonthlyReminderRule
import com.ygoular.keepit.reminder.domain.OneTimeReminderRule
import com.ygoular.keepit.reminder.domain.ReminderNote
import com.ygoular.keepit.reminder.domain.WeeklyReminderRule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber

internal const val NOTE_COLUMNS = """id, title, description, start_date, local_time, zone_id,
    interval_months, ordinal, weekday, occurrence_count, revision, consumed, rule_kind, interval_weeks, weekdays"""

internal fun SQLiteStatement.readStoredNote(): StoredNote {
    val date = LocalDate.parse(getText(3))
    val time = LocalTime.parse(getText(4))
    val zone = getText(5)
    val count = getInt(9)
    val rule = when (val kind = getText(12)) {
        "monthly" -> MonthlyReminderRule(date, time, zone, getInt(6), getInt(7), DayOfWeek.valueOf(getText(8)), count)
        "weekly" -> WeeklyReminderRule(
            date, time, zone, getInt(13),
            getText(14).split(',').mapTo(mutableSetOf()) { DayOfWeek.valueOf(it) },
            count,
        )
        "once" -> OneTimeReminderRule(date, time, zone)
        else -> error("Unknown stored reminder rule kind: $kind")
    }
    return StoredNote(ReminderNote(getText(0), getText(1), getText(2), rule), getText(10), getInt(11))
}

internal fun SQLiteStatement.bindStoredNote(row: StoredNote) {
    val note = row.note
    val rule = note.rule
    bindText(1, note.id)
    bindText(2, note.title)
    bindText(3, note.description)
    bindText(4, rule.startDate.toString())
    bindText(5, rule.time.toString())
    bindText(6, rule.zoneId)
    bindInt(10, rule.count)
    bindText(11, row.revision)
    bindInt(12, row.consumed)
    when (rule) {
        is MonthlyReminderRule -> {
            bindInt(7, rule.intervalMonths)
            bindInt(8, rule.ordinal)
            bindText(9, rule.weekday.name)
            bindText(13, "monthly")
            bindNull(14)
            bindNull(15)
        }
        is OneTimeReminderRule -> {
            bindNull(7)
            bindNull(8)
            bindNull(9)
            bindText(13, "once")
            bindNull(14)
            bindNull(15)
        }
        is WeeklyReminderRule -> {
            bindNull(7)
            bindNull(8)
            bindNull(9)
            bindText(13, "weekly")
            bindInt(14, rule.intervalWeeks)
            bindText(15, rule.weekdays.sortedBy { it.isoDayNumber }.joinToString(",") { it.name })
        }
    }
}
