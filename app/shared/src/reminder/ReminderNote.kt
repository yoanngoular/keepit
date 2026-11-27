package com.ygoular.keepit.reminder

data class ReminderNote(
    val id: String,
    val title: String,
    val description: String,
    val rule: MonthlyReminderRule,
)
