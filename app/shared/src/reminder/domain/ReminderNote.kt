package com.ygoular.keepit.reminder.domain

data class ReminderNote(
    val id: String,
    val title: String,
    val description: String,
    val rule: ReminderRule,
)
