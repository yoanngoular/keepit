package com.ygoular.keepit.reminder.domain

import kotlin.time.Instant

data class ReminderOccurrence(val number: Int, val at: Instant)
