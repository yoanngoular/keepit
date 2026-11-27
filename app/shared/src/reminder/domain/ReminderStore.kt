package com.ygoular.keepit.reminder.domain

interface ReminderStore {
    suspend fun list(): List<ReminderNote>
    suspend fun save(note: ReminderNote)
    suspend fun delete(id: String)
}
