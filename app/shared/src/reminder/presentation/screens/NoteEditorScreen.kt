package com.ygoular.keepit.reminder.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ygoular.keepit.reminder.domain.MonthlyReminderRule
import com.ygoular.keepit.reminder.domain.ReminderNote
import com.ygoular.keepit.reminder.domain.ReminderRule
import com.ygoular.keepit.reminder.presentation.components.ErrorText
import com.ygoular.keepit.reminder.presentation.components.Headline
import com.ygoular.keepit.reminder.presentation.components.Panel
import com.ygoular.keepit.reminder.presentation.components.ReminderDateTimeFields
import com.ygoular.keepit.reminder.presentation.components.ReminderEndDateField
import com.ygoular.keepit.reminder.presentation.components.WeeklyDayPicker
import com.ygoular.keepit.reminder.presentation.components.upcomingOccurrences
import com.ygoular.keepit.reminder.presentation.format.occurrenceLabel
import com.ygoular.keepit.reminder.presentation.format.ordinalLabel
import com.ygoular.keepit.reminder.presentation.format.weekdayLabel
import com.ygoular.keepit.reminder.presentation.model.MonthlyPattern
import com.ygoular.keepit.reminder.presentation.model.NoteDraft
import com.ygoular.keepit.reminder.presentation.model.RepeatCadence
import com.ygoular.keepit.reminder.domain.isPastOneTimeChange
import com.ygoular.keepit.resources.*
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
internal fun NoteEditorScreen(
    draft: NoteDraft,
    originalRule: ReminderRule?,
    now: Instant,
    saving: Boolean,
    failure: StringResource?,
    onChange: (NoteDraft) -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onSave: (ReminderNote) -> Unit,
) {
    val rule = remember(
        draft.date, draft.time, draft.zoneId, draft.interval, draft.ordinal, draft.weekday, draft.endDate,
        draft.cadence, draft.intervalWeeks, draft.weeklyDays, draft.intervalDays, draft.intervalYears,
        draft.monthlyPattern, draft.dayOfMonth, draft.hasReminder,
    ) {
        draft.parseRule()
    }
    val hasOccurrence = remember(rule) { runCatching { rule?.occurrences()?.any() == true }.getOrDefault(false) }
    val startDate = remember(draft.date) { runCatching { LocalDate.parse(draft.date.trim()) }.getOrNull() }
    val endBeforeStart = draft.cadence != RepeatCadence.ONCE && startDate != null &&
        draft.endDate?.let { it < startDate } == true
    val hasContent = draft.title.isNotBlank() || draft.description.isNotBlank()
    var rejectedOnSave by remember(draft) { mutableStateOf(false) }
    val pastOneTime = rejectedOnSave || rule?.isPastOneTimeChange(originalRule, now) == true
    val canSave = hasContent && (!draft.hasReminder || (hasOccurrence && !pastOneTime))
    var showTimeZone by rememberSaveable(draft.id, draft.hasReminder) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onClose, enabled = !saving) { Text(stringResource(Res.string.reminder_back)) }
            if (!draft.isNew) {
                TextButton(onClick = onDelete, enabled = !saving) {
                    Text(stringResource(Res.string.reminder_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Headline(stringResource(if (draft.isNew) Res.string.reminder_new_note else Res.string.reminder_edit_note))
            Panel {
                Field(Res.string.reminder_title, draft.title, { onChange(draft.copy(title = it)) }, enabled = !saving)
                Field(
                    Res.string.reminder_description, draft.description, { onChange(draft.copy(description = it)) },
                    enabled = !saving,
                    multiline = true,
                )
                if (!hasContent) {
                    Text(stringResource(Res.string.reminder_content_hint), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!draft.hasReminder) {
                OutlinedButton(onClick = { onChange(draft.addReminder()) }, enabled = !saving) {
                    Text(stringResource(Res.string.reminder_add))
                }
            } else Panel {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(Res.string.reminder_schedule),
                        Modifier.weight(1f).semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    TextButton(onClick = { onChange(draft.copy(hasReminder = false)) }, enabled = !saving) {
                        Text(stringResource(Res.string.reminder_remove))
                    }
                }
                ReminderDateTimeFields(
                    date = draft.date,
                    time = draft.time,
                    onDateChange = { onChange(draft.copy(date = it)) },
                    onTimeChange = { onChange(draft.copy(time = it)) },
                    enabled = !saving,
                )
                Choice(
                    label = Res.string.reminder_repeat,
                    options = listOf(
                        stringResource(Res.string.reminder_repeat_off),
                        stringResource(Res.string.reminder_daily),
                        stringResource(Res.string.reminder_weekly),
                        stringResource(Res.string.reminder_monthly),
                        stringResource(Res.string.reminder_yearly),
                    ),
                    selected = draft.cadence.ordinal,
                    onChoose = { onChange(draft.copy(cadence = RepeatCadence.entries[it])) },
                    enabled = !saving,
                )
                when (draft.cadence) {
                    RepeatCadence.ONCE -> Unit
                    RepeatCadence.DAILY -> Field(
                        Res.string.reminder_interval_days, draft.intervalDays,
                        { onChange(draft.copy(intervalDays = it)) }, enabled = !saving, numeric = true,
                    )
                    RepeatCadence.YEARLY -> {
                        Field(
                            Res.string.reminder_interval_years, draft.intervalYears,
                            { onChange(draft.copy(intervalYears = it)) }, enabled = !saving, numeric = true,
                        )
                        if (startDate?.month == Month.FEBRUARY && startDate.day == 29) {
                            Text(
                                stringResource(Res.string.reminder_leap_day_hint),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    RepeatCadence.MONTHLY -> MonthlySettings(draft, onChange, enabled = !saving)
                    RepeatCadence.WEEKLY -> {
                        Field(
                            Res.string.reminder_interval_weeks, draft.intervalWeeks,
                            { onChange(draft.copy(intervalWeeks = it)) }, enabled = !saving, numeric = true,
                        )
                        WeeklyDayPicker(
                            selected = draft.weeklyDays,
                            onToggle = { day ->
                                val days = if (day in draft.weeklyDays) draft.weeklyDays - day else draft.weeklyDays + day
                                onChange(draft.copy(weeklyDays = days))
                            },
                            enabled = !saving,
                        )
                    }
                }
                if (draft.cadence != RepeatCadence.ONCE) {
                    ReminderEndDateField(
                        endDate = draft.endDate,
                        startDate = startDate,
                        onChange = { onChange(draft.copy(endDate = it)) },
                        enabled = !saving,
                    )
                }
                TextButton(onClick = { showTimeZone = !showTimeZone }, enabled = !saving) {
                    Text(stringResource(Res.string.reminder_zone))
                }
                if (showTimeZone) {
                    Field(Res.string.reminder_zone, draft.zoneId, { onChange(draft.copy(zoneId = it)) }, enabled = !saving)
                }
                if (pastOneTime) {
                    ErrorText(Res.string.reminder_future_required)
                } else if (draft.cadence == RepeatCadence.WEEKLY && draft.weeklyDays.isEmpty()) {
                    ErrorText(Res.string.reminder_weekdays_required)
                } else if (endBeforeStart) {
                    ErrorText(Res.string.reminder_end_before_start)
                } else if (rule == null || !hasOccurrence) {
                    ErrorText(Res.string.reminder_invalid_rule)
                }
            }
            if (draft.cadence != RepeatCadence.ONCE && rule != null && hasOccurrence) {
                Panel(tinted = true) {
                    Text(
                        stringResource(Res.string.reminder_preview),
                        Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        stringResource(Res.string.reminder_preview_description, rule.zoneId),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    val upcoming = upcomingOccurrences(rule, now, limit = 7)
                    if (upcoming == null) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                    } else {
                        if (upcoming.isEmpty()) Text(stringResource(Res.string.reminder_no_upcoming))
                        upcoming.take(6).forEach { occurrence ->
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(occurrenceLabel(occurrence, rule), style = MaterialTheme.typography.bodyLarge)
                        }
                        if (upcoming.size > 6) {
                            Text(stringResource(Res.string.reminder_more_dates), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (failure != null) ErrorText(failure)
            Box(Modifier.padding(bottom = 8.dp))
        }
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
            Button(
                onClick = {
                    if (rule?.isPastOneTimeChange(originalRule, Clock.System.now()) == true) {
                        rejectedOnSave = true
                    } else {
                        onSave(draft.toNote(rule))
                    }
                },
                enabled = canSave && !saving,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(stringResource(if (saving) Res.string.reminder_saving else Res.string.reminder_save))
            }
        }
    }
}

@Composable
private fun MonthlySettings(draft: NoteDraft, onChange: (NoteDraft) -> Unit, enabled: Boolean) {
    Field(
        Res.string.reminder_interval, draft.interval, { onChange(draft.copy(interval = it)) },
        enabled = enabled, numeric = true,
    )
    Choice(
        label = Res.string.reminder_monthly_pattern,
        options = listOf(
            stringResource(Res.string.reminder_monthly_by_weekday),
            stringResource(Res.string.reminder_monthly_by_day),
        ),
        selected = draft.monthlyPattern.ordinal,
        onChoose = { onChange(draft.copy(monthlyPattern = MonthlyPattern.entries[it])) },
        enabled = enabled,
    )
    if (draft.monthlyPattern == MonthlyPattern.DAY_OF_MONTH) {
        Field(
            Res.string.reminder_day_of_month, draft.dayOfMonth, { onChange(draft.copy(dayOfMonth = it)) },
            enabled = enabled, numeric = true,
        )
        Text(stringResource(Res.string.reminder_short_month_hint), style = MaterialTheme.typography.bodySmall)
        return
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val ordinals = (1..5).toList() + MonthlyReminderRule.LAST
        Choice(
            Res.string.reminder_ordinal, ordinals.map { ordinalLabel(it) }, ordinals.indexOf(draft.ordinal),
            onChoose = { onChange(draft.copy(ordinal = ordinals[it])) },
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        val weekdays = DayOfWeek.entries.map { weekdayLabel(it) }
        Choice(
            Res.string.reminder_weekday, weekdays, draft.weekday.ordinal,
            onChoose = { onChange(draft.copy(weekday = DayOfWeek.entries[it])) },
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Field(
    label: StringResource,
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,

    multiline: Boolean = false,
    numeric: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },

        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        singleLine = !multiline,
        minLines = if (multiline) 3 else 1,
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun Choice(
    label: StringResource,
    options: List<String>,
    selected: Int,
    onChoose: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val controlLabel = stringResource(label)
    Column(modifier) {
        Text(controlLabel, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = controlLabel
                    stateDescription = options[selected]
                },
            ) {
                Text(options[selected])
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = {
                        expanded = false
                        onChoose(index)
                    })
                }
            }
        }
    }
}
