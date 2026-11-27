package com.ygoular.keepit.reminder

interface ReminderStore {
    suspend fun list(): List<ReminderNote>
    suspend fun save(note: ReminderNote)
    suspend fun delete(id: String)
}
