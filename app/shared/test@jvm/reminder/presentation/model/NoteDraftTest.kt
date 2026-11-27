package com.ygoular.keepit.reminder.presentation.model

import androidx.compose.runtime.saveable.SaverScope
import com.ygoular.keepit.reminder.domain.MonthlyReminderRule
import com.ygoular.keepit.reminder.domain.OneTimeReminderRule
import com.ygoular.keepit.reminder.domain.WeeklyReminderRule
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class NoteDraftTest {
    @Test
    fun `default schedule remains upcoming during the repeated autumn hour`() {
        val now = Instant.parse("2026-10-25T00:30:00Z")
        val draft = NoteDraft.new(now = now, zone = TimeZone.of("Europe/Paris"))
        val rule = assertIs<OneTimeReminderRule>(draft.parseRule())
        assertEquals(Instant.parse("2026-10-25T02:00:00Z"), rule.occurrences().single().at)
    }

    @Test
    fun `new note defaults to one reminder and ignores repeat settings`() {
        val draft = NoteDraft.new().copy(
            date = "2026-12-02", time = "17:20", zoneId = "UTC", count = "",
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
        val draft = NoteDraft.new().copy(zoneId = "UTC", cadence = RepeatCadence.MONTHLY)
        val oldFields = listOf(
            draft.id, draft.title, draft.description, draft.date, draft.time, draft.zoneId,
            draft.interval, draft.ordinal.toString(), draft.weekday.name, draft.count, draft.isNew.toString(),
        )
        val restored = assertNotNull(NoteDraft.saver.restore(ArrayList(oldFields)))
        val expectedRule = assertIs<MonthlyReminderRule>(draft.parseRule())
        assertEquals(expectedRule, restored.parseRule())
        assertEquals(RepeatCadence.MONTHLY, NoteDraft.fromNote(draft.toNote(expectedRule)).cadence)
    }

    @Test
    fun `restored new draft reuses its note identity for a save retry`() {
        val draft = NoteDraft.new().copy(title = "Remember this", zoneId = "UTC")
        val firstSave = draft.toNote(assertNotNull(draft.parseRule()))

        val savedState = with(NoteDraft.saver) { assertNotNull(SaverScope { true }.save(draft)) }
        val restored = assertNotNull(NoteDraft.saver.restore(savedState))
        val retriedSave = restored.toNote(assertNotNull(restored.parseRule()))

        assertTrue(restored.isNew)
        assertEquals(firstSave, retriedSave)
    }

    @Test
    fun `weekly selection survives saving and reopening the note`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)
        val draft = NoteDraft.new().copy(
            zoneId = "UTC",
            cadence = RepeatCadence.WEEKLY,
            intervalWeeks = "3",
            weeklyDays = days,
        )
        val savedState = with(NoteDraft.saver) { assertNotNull(SaverScope { true }.save(draft)) }
        val restored = assertNotNull(NoteDraft.saver.restore(savedState))
        val rule = assertIs<WeeklyReminderRule>(restored.parseRule())

        assertEquals(draft, restored)
        assertEquals(3, rule.intervalWeeks)
        assertEquals(days, rule.weekdays)
        assertEquals(rule, NoteDraft.fromNote(restored.toNote(rule)).parseRule())
    }

    @Test
    fun `empty weekly selection is invalid and does not affect monthly settings`() {
        val draft = NoteDraft.new().copy(zoneId = "UTC", cadence = RepeatCadence.WEEKLY, weeklyDays = emptySet())
        assertNull(draft.parseRule())
        assertIs<MonthlyReminderRule>(draft.copy(cadence = RepeatCadence.MONTHLY).parseRule())
    }
}
