package com.ygoular.keepit.reminder.storage

import com.ygoular.keepit.reminder.domain.ReminderNote

data class StoredNote(val note: ReminderNote, val revision: String?, val consumed: Int) {
    init {
        if (note.rule == null) {
            require(revision == null && consumed == 0) { "A note without a reminder cannot record alarm state." }
        } else {
            require(revision != null) { "A reminder requires a revision." }
        }
    }
}
