package com.ygoular.keepit.reminder.presentation.model

import androidx.compose.runtime.saveable.SaverScope
import com.ygoular.keepit.reminder.domain.DailyReminderRule
import com.ygoular.keepit.reminder.domain.MonthlyDayReminderRule
import com.ygoular.keepit.reminder.domain.MonthlyReminderRule
import com.ygoular.keepit.reminder.domain.OneTimeReminderRule
import com.ygoular.keepit.reminder.domain.WeeklyReminderRule
import com.ygoular.keepit.reminder.domain.YearlyReminderRule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class NoteDraftTest {
    @Test
    fun `new notes save without a reminder and existing reminders can be removed`() {
        val draft = NoteDraft.new().copy(description = "A plain note")
        assertFalse(draft.hasReminder)
        val plain = draft.toNote(draft.parseRule())
        assertNull(plain.rule)
        assertFalse(NoteDraft.fromNote(plain).hasReminder)

        val now = Instant.parse("2026-12-02T17:20:00Z")
        val scheduled = draft.addReminder(now = now, zone = TimeZone.UTC)
        val rule = assertIs<OneTimeReminderRule>(scheduled.parseRule())
        assertEquals(Instant.parse("2026-12-02T18:00:00Z"), rule.occurrences().single().at)
        val reopened = NoteDraft.fromNote(scheduled.toNote(rule))
        assertTrue(reopened.hasReminder)
        assertEquals(rule, reopened.parseRule())
        val removed = reopened.copy(hasReminder = false)
        val savedDraft = with(NoteDraft.saver) { assertNotNull(SaverScope { true }.save(removed)) }
        val restored = assertNotNull(NoteDraft.saver.restore(savedDraft))
        assertNull(restored.parseRule())
        assertNull(restored.toNote(rule).rule)
        assertEquals(draft.id, restored.id)
    }

    @Test
    fun `new cadence options keep their settings after draft restoration and note reopening`() {
        val base = NoteDraft.new().addReminder().copy(
            zoneId = "UTC", date = "2026-12-01", endDate = LocalDate(2028, 12, 31),
        )
        val daily = base.copy(cadence = RepeatCadence.DAILY, intervalDays = "3")
        val yearly = base.copy(cadence = RepeatCadence.YEARLY, intervalYears = "2")
        val monthlyDay = base.copy(
            cadence = RepeatCadence.MONTHLY, monthlyPattern = MonthlyPattern.DAY_OF_MONTH, dayOfMonth = "31",
        )
        val lastWeekday = base.copy(cadence = RepeatCadence.MONTHLY, ordinal = MonthlyReminderRule.LAST)

        for (draft in listOf(daily, yearly, monthlyDay, lastWeekday)) {
            val rule = assertNotNull(draft.parseRule())
            val saved = with(NoteDraft.saver) { assertNotNull(SaverScope { true }.save(draft)) }
            val restored = assertNotNull(NoteDraft.saver.restore(saved))
            assertEquals(draft, restored)
            assertEquals(rule, restored.parseRule())
            assertEquals(rule, NoteDraft.fromNote(draft.toNote(rule)).parseRule())
        }
        assertEquals(3, assertIs<DailyReminderRule>(daily.parseRule()).intervalDays)
        assertEquals(2, assertIs<YearlyReminderRule>(yearly.parseRule()).intervalYears)
        assertEquals(31, assertIs<MonthlyDayReminderRule>(monthlyDay.parseRule()).dayOfMonth)
        assertEquals(MonthlyReminderRule.LAST, assertIs<MonthlyReminderRule>(lastWeekday.parseRule()).ordinal)
        assertNull(monthlyDay.copy(dayOfMonth = "32").parseRule())
    }

    @Test
    fun `default schedule remains upcoming during the repeated autumn hour`() {
        val now = Instant.parse("2026-10-25T00:30:00Z")
        val draft = NoteDraft.new().addReminder(now = now, zone = TimeZone.of("Europe/Paris"))
        val rule = assertIs<OneTimeReminderRule>(draft.parseRule())
        assertEquals(Instant.parse("2026-10-25T02:00:00Z"), rule.occurrences().single().at)
    }

    @Test
    fun `adding a reminder defaults to one occurrence and ignores repeat settings`() {
        val draft = NoteDraft.new().addReminder().copy(
            date = "2026-12-02", time = "17:20", zoneId = "UTC", endDate = LocalDate(2026, 12, 1),
        )
        assertEquals(RepeatCadence.ONCE, draft.cadence)
        val rule = OneTimeReminderRule(LocalDate(2026, 12, 2), LocalTime(17, 20), "UTC")
        assertEquals(rule, draft.parseRule())
        val reopened = NoteDraft.fromNote(draft.toNote(rule))
        assertEquals(rule, reopened.parseRule())
        assertEquals(draft.id, reopened.id)
    }

    @Test
    fun `previous monthly drafts without a saved cadence still restore as monthly`() {
        val draft = NoteDraft.new().addReminder().copy(
            date = "2026-10-01", zoneId = "UTC", cadence = RepeatCadence.MONTHLY,
        )
        val oldFields = listOf(
            draft.id, draft.title, draft.description, draft.date, draft.time, draft.zoneId,
            draft.interval, draft.ordinal.toString(), draft.weekday.name, "2", draft.isNew.toString(),
        )
        val restored = assertNotNull(NoteDraft.saver.restore(ArrayList(oldFields)))
        val expectedRule = assertIs<MonthlyReminderRule>(
            draft.copy(endDate = LocalDate(2026, 12, 8)).parseRule(),
        )
        assertEquals(LocalDate(2026, 12, 8), restored.endDate)
        assertEquals(expectedRule, restored.parseRule())
        assertEquals(RepeatCadence.MONTHLY, NoteDraft.fromNote(draft.toNote(expectedRule)).cadence)
    }

    @Test
    fun `restored new draft reuses its note identity for a save retry`() {
        val draft = NoteDraft.new().copy(title = "Remember this")
        val firstSave = draft.toNote(draft.parseRule())

        val savedState = with(NoteDraft.saver) { assertNotNull(SaverScope { true }.save(draft)) }
        val restored = assertNotNull(NoteDraft.saver.restore(savedState))
        val retriedSave = restored.toNote(restored.parseRule())

        assertTrue(restored.isNew)
        assertEquals(firstSave, retriedSave)
        assertFalse(restored.hasReminder)
        assertNull(retriedSave.rule)
    }

    @Test
    fun `weekly selection survives saving and reopening the note`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
        val draft = NoteDraft.new().addReminder().copy(
            zoneId = "UTC",
            cadence = RepeatCadence.WEEKLY,
            date = "2026-12-02",
            intervalWeeks = "3",
            weeklyDays = days,
            endDate = LocalDate(2027, 1, 2),
        )
        val savedState = with(NoteDraft.saver) { assertNotNull(SaverScope { true }.save(draft)) }
        val restored = assertNotNull(NoteDraft.saver.restore(savedState))
        val rule = assertIs<WeeklyReminderRule>(restored.parseRule())

        assertEquals(draft, restored)
        assertEquals(3, rule.intervalWeeks)
        assertEquals(days, rule.weekdays)
        assertEquals(draft.endDate, rule.endDate)
        val reopened = NoteDraft.fromNote(restored.toNote(rule))
        assertEquals(rule, reopened.parseRule())
        val unbounded = assertIs<WeeklyReminderRule>(reopened.copy(endDate = null).parseRule())
        assertNull(unbounded.endDate)
    }

    @Test
    fun `end date may match the start but cannot precede it`() {
        val draft = NoteDraft.new().addReminder().copy(
            date = "2026-12-03",
            zoneId = "UTC",
            cadence = RepeatCadence.WEEKLY,
            weeklyDays = setOf(DayOfWeek.THURSDAY),
        )
        assertNull(draft.copy(endDate = LocalDate(2026, 12, 2)).parseRule())
        val sameDay = assertIs<WeeklyReminderRule>(draft.copy(endDate = LocalDate(2026, 12, 3)).parseRule())
        assertEquals(1, sameDay.occurrences().count())
    }

    @Test
    fun `empty weekly selection is invalid and does not affect monthly settings`() {
        val draft = NoteDraft.new().addReminder().copy(zoneId = "UTC", cadence = RepeatCadence.WEEKLY, weeklyDays = emptySet())
        assertNull(draft.parseRule())
        assertIs<MonthlyReminderRule>(draft.copy(cadence = RepeatCadence.MONTHLY).parseRule())
    }
}
