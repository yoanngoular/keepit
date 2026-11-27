package com.ygoular.keepit.reminder.presentation.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import com.ygoular.keepit.resources.Res
import com.ygoular.keepit.resources.reminder_cancel
import com.ygoular.keepit.resources.reminder_picker_done
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderDatePickerDialog(
    initialDate: LocalDate?,
    enabled: Boolean,
    onSelect: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val year = initialDate?.year ?: 2000
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
        yearRange = minOf(1900, year)..maxOf(2100, year),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = enabled && picker.selectedDateMillis != null,
                onClick = {
                    picker.selectedDateMillis?.let { millis ->
                        onSelect(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                    }
                    onDismiss()
                },
            ) { Text(stringResource(Res.string.reminder_picker_done)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.reminder_cancel)) }
        },
    ) { DatePicker(state = picker) }
}
