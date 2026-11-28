package com.ygoular.keepit.reminder.storage

import androidx.sqlite.SQLiteStatement
import com.ygoular.keepit.reminder.domain.DailyReminderRule
import com.ygoular.keepit.reminder.domain.MonthlyDayReminderRule
import com.ygoular.keepit.reminder.domain.MonthlyReminderRule
import com.ygoular.keepit.reminder.domain.OneTimeReminderRule
import com.ygoular.keepit.reminder.domain.ReminderNote
import com.ygoular.keepit.reminder.domain.ReminderRule
import com.ygoular.keepit.reminder.domain.WeeklyReminderRule
import com.ygoular.keepit.reminder.domain.YearlyReminderRule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber

internal const val REMINDER_COLUMNS = """start_date, local_time, zone_id,
    interval_months, ordinal, weekday, end_date, revision, consumed, rule_kind, interval_weeks, weekdays,
    interval_days, interval_years, day_of_month"""

internal const val REMINDER_VALUES = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?"

internal fun SQLiteStatement.readStoredNote(): StoredNote {
    if (isNull(12)) return StoredNote(ReminderNote(getText(0), getText(1), getText(2), null), null, 0)

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

internal fun SQLiteStatement.bindReminderDetails(rule: ReminderRule, revision: String, consumed: Int, offset: Int) {
    bindText(offset + 1, rule.startDate.toString())
    bindText(offset + 2, rule.time.toString())
    bindText(offset + 3, rule.zoneId)
    bindText(offset + 8, revision)
    bindInt(offset + 9, consumed)

    // Clear every optional field before binding the applicable cadence.
    for (index in 4..7) bindNull(offset + index)
    for (index in 11..15) bindNull(offset + index)
    val kind = when (rule) {
        is MonthlyReminderRule -> {
            bindInt(offset + 4, rule.intervalMonths)
            bindInt(offset + 5, rule.ordinal)
            bindText(offset + 6, rule.weekday.name)
            bindEndDate(offset, rule.endDate)
            "monthly"
        }
        is OneTimeReminderRule -> "once"
        is WeeklyReminderRule -> {
            bindEndDate(offset, rule.endDate)
            bindInt(offset + 11, rule.intervalWeeks)
            bindText(offset + 12, rule.weekdays.sortedBy { it.isoDayNumber }.joinToString(",") { it.name })
            "weekly"
        }
        is DailyReminderRule -> {
            bindEndDate(offset, rule.endDate)
            bindInt(offset + 13, rule.intervalDays)
            "daily"
        }
        is YearlyReminderRule -> {
            bindEndDate(offset, rule.endDate)
            bindInt(offset + 14, rule.intervalYears)
            "yearly"
        }
        is MonthlyDayReminderRule -> {
            bindInt(offset + 4, rule.intervalMonths)
            bindEndDate(offset, rule.endDate)
            bindInt(offset + 15, rule.dayOfMonth)
            "monthly_day"
        }
    }
    bindText(offset + 10, kind)
}

private fun SQLiteStatement.bindEndDate(offset: Int, value: LocalDate?) {
    if (value != null) bindText(offset + 7, value.toString())
}
