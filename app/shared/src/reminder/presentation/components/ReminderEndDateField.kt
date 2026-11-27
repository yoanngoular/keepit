package com.ygoular.keepit.reminder.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ygoular.keepit.reminder.presentation.format.reminderDateLabel
import com.ygoular.keepit.resources.Res
import com.ygoular.keepit.resources.reminder_clear_end_date
import com.ygoular.keepit.resources.reminder_end_date
import com.ygoular.keepit.resources.reminder_no_end_date
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReminderEndDateField(
    endDate: LocalDate?,
    startDate: LocalDate?,
    onChange: (LocalDate?) -> Unit,
    enabled: Boolean,
) {
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        PickerButton(
            label = stringResource(Res.string.reminder_end_date),
            value = if (endDate == null) stringResource(Res.string.reminder_no_end_date) else reminderDateLabel(endDate),
            onClick = { pickingDate = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        if (endDate != null) {
            TextButton(onClick = { onChange(null) }, enabled = enabled) {
                Text(stringResource(Res.string.reminder_clear_end_date))
            }
        }
    }
    if (pickingDate) {
        ReminderDatePickerDialog(
            initialDate = endDate ?: startDate,
            enabled = enabled,
            onSelect = onChange,
            onDismiss = { pickingDate = false },
        )
    }
}
