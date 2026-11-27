package com.ygoular.keepit.reminder.storage

import com.ygoular.keepit.reminder.ReminderNote

data class StoredNote(val note: ReminderNote, val revision: String, val consumed: Int)
