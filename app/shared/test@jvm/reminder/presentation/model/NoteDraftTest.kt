package com.ygoular.keepit.reminder.presentation.model

import androidx.compose.runtime.saveable.SaverScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
}
