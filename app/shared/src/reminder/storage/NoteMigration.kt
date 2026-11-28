package com.ygoular.keepit.reminder.storage

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.async.executeSQL
import androidx.sqlite.async.prepare
import androidx.sqlite.async.step
import com.ygoular.keepit.reminder.domain.MonthlyReminderRule
import com.ygoular.keepit.reminder.domain.OneTimeReminderRule
import com.ygoular.keepit.reminder.domain.ReminderNote
import com.ygoular.keepit.reminder.domain.WeeklyReminderRule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

private const val LEGACY_COLUMNS = """id, title, description, start_date, local_time, zone_id,
    interval_months, ordinal, weekday, occurrence_count, revision, consumed"""

private const val VERSION_FOUR_COLUMNS = """id, title, description, start_date, local_time, zone_id,
    interval_months, ordinal, weekday, end_date, revision, consumed, rule_kind, interval_weeks, weekdays"""

internal suspend fun SQLiteConnection.copyLegacyNotes(version: Int) {
    if (version == 4) {
        executeSQL(
            """INSERT INTO notes_v5 (rowid, $NOTE_COLUMNS)
                SELECT rowid, $VERSION_FOUR_COLUMNS, NULL, NULL, NULL FROM notes"""
        )
        return
    }
    val legacyRuleColumns = when (version) {
        1 -> "'monthly', NULL, NULL"
        2, 3 -> "rule_kind, interval_weeks, weekdays"
        else -> error("Unsupported KeepIt schema version: $version")
    }
    prepare("SELECT $LEGACY_COLUMNS, $legacyRuleColumns, rowid FROM notes").use { source ->
        prepare("INSERT INTO notes_v5 ($NOTE_COLUMNS, rowid) VALUES ($NOTE_VALUES, ?)").use { target ->
            while (source.step()) {
                target.bindStoredNote(source.readLegacyNote())
                target.bindLong(19, source.getLong(15))
                target.step()
                target.reset()
            }
        }
    }
}

private fun SQLiteStatement.readLegacyNote(): StoredNote {
    val date = LocalDate.parse(getText(3))
    val time = LocalTime.parse(getText(4))
    val zone = getText(5)
    val count = getInt(9)
    require(count in 1..100) { "Invalid legacy reminder occurrence count" }
    val rule = when (val kind = getText(12)) {
        "monthly" -> {
            val unlimited = MonthlyReminderRule(date, time, zone, getInt(6), getInt(7), DayOfWeek.valueOf(getText(8)), null)
            unlimited.copy(endDate = unlimited.scheduledDates().elementAtOrNull(count - 1))
        }
        "weekly" -> {
            val weekdays = getText(14).split(',').mapTo(mutableSetOf()) { DayOfWeek.valueOf(it) }
            val unlimited = WeeklyReminderRule(date, time, zone, getInt(13), weekdays, null)
            unlimited.copy(endDate = unlimited.scheduledDates().elementAtOrNull(count - 1))
        }
        "once" -> {
            require(count == 1) { "Invalid legacy one-time reminder occurrence count" }
            OneTimeReminderRule(date, time, zone)
        }
        else -> error("Unknown legacy reminder rule kind: $kind")
    }
    return StoredNote(ReminderNote(getText(0), getText(1), getText(2), rule), getText(10), getInt(11))
}
