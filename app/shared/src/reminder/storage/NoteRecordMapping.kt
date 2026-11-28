package com.ygoular.keepit.reminder.storage

import androidx.sqlite.SQLiteStatement
import com.ygoular.keepit.reminder.domain.DailyReminderRule
import com.ygoular.keepit.reminder.domain.MonthlyDayReminderRule
import com.ygoular.keepit.reminder.domain.MonthlyReminderRule
import com.ygoular.keepit.reminder.domain.OneTimeReminderRule
import com.ygoular.keepit.reminder.domain.ReminderNote
import com.ygoular.keepit.reminder.domain.WeeklyReminderRule
import com.ygoular.keepit.reminder.domain.YearlyReminderRule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber

internal const val NOTE_COLUMNS = """id, title, description, start_date, local_time, zone_id,
    interval_months, ordinal, weekday, end_date, revision, consumed, rule_kind, interval_weeks, weekdays,
    interval_days, interval_years, day_of_month"""

internal const val NOTE_VALUES = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"

internal fun SQLiteStatement.readStoredNote(): StoredNote {
    val date = LocalDate.parse(getText(3))
    val time = LocalTime.parse(getText(4))
    val zone = getText(5)
    val endDate = if (isNull(9)) null else LocalDate.parse(getText(9))
    val rule = when (val kind = getText(12)) {
        "monthly" -> MonthlyReminderRule(date, time, zone, getInt(6), getInt(7), DayOfWeek.valueOf(getText(8)), endDate)
        "weekly" -> WeeklyReminderRule(
            date, time, zone, getInt(13),
            getText(14).split(',').mapTo(mutableSetOf()) { DayOfWeek.valueOf(it) },
            endDate,
        )
        "once" -> OneTimeReminderRule(date, time, zone)
        "daily" -> DailyReminderRule(date, time, zone, getInt(15), endDate)
        "yearly" -> YearlyReminderRule(date, time, zone, getInt(16), endDate)
        "monthly_day" -> MonthlyDayReminderRule(date, time, zone, getInt(6), getInt(17), endDate)
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
    bindText(11, row.revision)
    bindInt(12, row.consumed)

    // Clear all optional fields when reusing a statement or replacing a different cadence.
    for (index in 7..10) bindNull(index)
    for (index in 14..18) bindNull(index)
    val kind = when (rule) {
        is MonthlyReminderRule -> {
            bindInt(7, rule.intervalMonths)
            bindInt(8, rule.ordinal)
            bindText(9, rule.weekday.name)
            bindEndDate(rule.endDate)
            "monthly"
        }
        is OneTimeReminderRule -> "once"
        is WeeklyReminderRule -> {
            bindEndDate(rule.endDate)
            bindInt(14, rule.intervalWeeks)
            bindText(15, rule.weekdays.sortedBy { it.isoDayNumber }.joinToString(",") { it.name })
            "weekly"
        }
        is DailyReminderRule -> {
            bindEndDate(rule.endDate)
            bindInt(16, rule.intervalDays)
            "daily"
        }
        is YearlyReminderRule -> {
            bindEndDate(rule.endDate)
            bindInt(17, rule.intervalYears)
            "yearly"
        }
        is MonthlyDayReminderRule -> {
            bindInt(7, rule.intervalMonths)
            bindEndDate(rule.endDate)
            bindInt(18, rule.dayOfMonth)
            "monthly_day"
        }
    }
    bindText(13, kind)
}

private fun SQLiteStatement.bindEndDate(value: LocalDate?) {
    if (value != null) bindText(10, value.toString())
}
