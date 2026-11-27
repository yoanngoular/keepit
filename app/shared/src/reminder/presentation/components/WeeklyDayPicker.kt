package com.ygoular.keepit.reminder.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.ygoular.keepit.reminder.presentation.format.weekdayLabel
import com.ygoular.keepit.resources.Res
import com.ygoular.keepit.resources.reminder_weekdays
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WeeklyDayPicker(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    enabled: Boolean,
) {
    Column {
        Text(stringResource(Res.string.reminder_weekdays), style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DayOfWeek.entries.forEach { day ->
                FilterChip(
                    selected = day in selected,
                    onClick = { onToggle(day) },
                    enabled = enabled,
                    label = { Text(weekdayLabel(day)) },
                )
            }
        }
    }
}
