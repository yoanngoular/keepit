package com.ygoular.keepit.reminder.storage

import com.ygoular.keepit.reminder.domain.ReminderNote

data class StoredNote(val note: ReminderNote, val revision: String, val consumed: Int)
