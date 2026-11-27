package com.ygoular.keepit.reminder.presentation.model

import androidx.compose.runtime.saveable.SaverScope
import com.ygoular.keepit.reminder.domain.MonthlyReminderRule
import com.ygoular.keepit.reminder.domain.WeeklyReminderRule
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NoteDraftTest {
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
