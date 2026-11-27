package com.ygoular.keepit.reminder.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.ygoular.keepit.reminder.domain.MonthlyReminderRule
import com.ygoular.keepit.reminder.domain.ReminderDeliveryPolicy
import com.ygoular.keepit.reminder.domain.ReminderOccurrence
import com.ygoular.keepit.reminder.domain.ReminderRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Instant

@Composable
internal fun upcomingOccurrences(rule: ReminderRule, now: Instant, limit: Int): List<ReminderOccurrence>? {
    if (rule !is MonthlyReminderRule || rule.ordinal != 5) {
        return remember(rule, now, limit) { ReminderDeliveryPolicy.upcoming(rule, now, limit) }
    }

    // Months without a fifth weekday need a slower search. Keep it outside composition.
    return key(rule, now, limit) {
        produceState<List<ReminderOccurrence>?>(null) {
            value = withContext(Dispatchers.Default) { ReminderDeliveryPolicy.upcoming(rule, now, limit) }
        }.value
    }
}
