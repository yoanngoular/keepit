package com.ygoular.keepit.reminder.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.ygoular.keepit.reminder.presentation.format.formatTime
import com.ygoular.keepit.reminder.presentation.format.reminderDateLabel
import com.ygoular.keepit.resources.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderDateTimeFields(
    date: String,
    time: String,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    enabled: Boolean,
) {
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
    val parsedTime = runCatching { LocalTime.parse(time) }.getOrNull()
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        PickerButton(
            label = stringResource(Res.string.reminder_start_date),
            value = if (parsedDate == null) date else reminderDateLabel(parsedDate),
            onClick = { pickingDate = true },
            enabled = enabled,
            modifier = Modifier.weight(1.4f),
        )
        PickerButton(
            label = stringResource(Res.string.reminder_time),
            value = parsedTime?.let(::formatTime) ?: time,
            onClick = { pickingTime = true },
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
    }
    if (pickingDate) {
        val year = parsedDate?.year ?: 2000
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = parsedDate?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
            yearRange = minOf(1900, year)..maxOf(2100, year),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(
                    enabled = enabled && picker.selectedDateMillis != null,
                    onClick = {
                        picker.selectedDateMillis?.let { millis ->
                            onDateChange(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date.toString())
                        }
                        pickingDate = false
                    },
                ) { Text(stringResource(Res.string.reminder_picker_done)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) { Text(stringResource(Res.string.reminder_cancel)) }
            },
        ) { DatePicker(state = picker) }
    }
    if (pickingTime) {
        val picker = rememberTimePickerState(
            initialHour = parsedTime?.hour ?: 9,
            initialMinute = parsedTime?.minute ?: 0,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text(stringResource(Res.string.reminder_time)) },
            text = { TimePicker(state = picker) },
            confirmButton = {
                TextButton(enabled = enabled, onClick = {
                    onTimeChange(LocalTime(picker.hour, picker.minute).toString())
                    pickingTime = false
                }) { Text(stringResource(Res.string.reminder_picker_done)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingTime = false }) { Text(stringResource(Res.string.reminder_cancel)) }
            },
        )
    }
}

@Composable
private fun PickerButton(label: String, value: String, onClick: () -> Unit, enabled: Boolean, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().semantics {
                contentDescription = label
                stateDescription = value
            },
        ) {
            Text(value)
        }
    }
}
