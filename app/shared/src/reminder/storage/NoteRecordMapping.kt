package com.ygoular.keepit.reminder.storage

import androidx.sqlite.SQLiteStatement
import com.ygoular.keepit.reminder.MonthlyReminderRule
import com.ygoular.keepit.reminder.ReminderNote
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

internal const val NOTE_COLUMNS = """id, title, description, start_date, local_time, zone_id,
    interval_months, ordinal, weekday, occurrence_count, revision, consumed"""

internal fun SQLiteStatement.readStoredNote(): StoredNote {
    val rule = MonthlyReminderRule(
        startDate = LocalDate.parse(getText(3)),
        time = LocalTime.parse(getText(4)),
        zoneId = getText(5),
        intervalMonths = getInt(6),
        ordinal = getInt(7),
        weekday = DayOfWeek.valueOf(getText(8)),
        count = getInt(9),
    )
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
    bindInt(7, rule.intervalMonths)
    bindInt(8, rule.ordinal)
    bindText(9, rule.weekday.name)
    bindInt(10, rule.count)
    bindText(11, row.revision)
    bindInt(12, row.consumed)
}
