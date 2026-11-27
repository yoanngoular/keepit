package com.ygoular.keepit.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ygoular.keepit.resources.*
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
internal fun NoteEditorScreen(
    draft: NoteDraft,
    now: Instant,
    saving: Boolean,
    failure: StringResource?,
    onChange: (NoteDraft) -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onSave: (ReminderNote) -> Unit,
) {
    val rule = remember(draft.date, draft.time, draft.zoneId, draft.interval, draft.ordinal, draft.weekday, draft.count) {
        draft.parseRule()
    }
    val occurrences = remember(rule) { rule?.let { runCatching(it::occurrences).getOrNull() }.orEmpty() }
    val hasContent = draft.title.isNotBlank() || draft.description.isNotBlank()
    val canSave = rule != null && occurrences.isNotEmpty() && hasContent
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
            Panel {
                Text(
                    stringResource(Res.string.reminder_repeat),
                    Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Field(
                        Res.string.reminder_start_date, draft.date, { onChange(draft.copy(date = it)) },
                        enabled = !saving,
                        modifier = Modifier.weight(1.3f),
                        placeholder = Res.string.reminder_date_hint,
                    )
                    Field(
                        Res.string.reminder_time, draft.time, { onChange(draft.copy(time = it)) },
                        enabled = !saving,
                        modifier = Modifier.weight(1f),
                        placeholder = Res.string.reminder_time_hint,
                    )
                }
                Field(Res.string.reminder_zone, draft.zoneId, { onChange(draft.copy(zoneId = it)) }, enabled = !saving)
                Field(Res.string.reminder_interval, draft.interval, { onChange(draft.copy(interval = it)) }, enabled = !saving, numeric = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val ordinals = (1..5).map { ordinalLabel(it) }
                    Choice(
                        Res.string.reminder_ordinal, ordinals, draft.ordinal - 1,
                        onChoose = { onChange(draft.copy(ordinal = it + 1)) },
                        enabled = !saving,
                        modifier = Modifier.weight(1f),
                    )
                    val weekdays = DayOfWeek.entries.map { weekdayLabel(it) }
                    Choice(
                        Res.string.reminder_weekday, weekdays, draft.weekday.ordinal,
                        onChoose = { onChange(draft.copy(weekday = DayOfWeek.entries[it])) },
                        enabled = !saving,
                        modifier = Modifier.weight(1f),
                    )
                }
                Field(Res.string.reminder_count, draft.count, { onChange(draft.copy(count = it)) }, enabled = !saving, numeric = true)
                if (rule == null || occurrences.isEmpty()) ErrorText(Res.string.reminder_invalid_rule)
            }
            if (rule != null && occurrences.isNotEmpty()) {
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
                    val upcoming = occurrences.filter { it.at >= now }
                    if (upcoming.isEmpty()) Text(stringResource(Res.string.reminder_no_upcoming))
                    upcoming.take(6).forEach { occurrence ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(occurrenceLabel(occurrence, rule), style = MaterialTheme.typography.bodyLarge)
                    }
                    if (upcoming.size > 6) {
                        Text(stringResource(Res.string.reminder_more_dates, upcoming.size - 6), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (failure != null) ErrorText(failure)
            Box(Modifier.padding(bottom = 8.dp))
        }
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
            Button(
                onClick = { if (rule != null) onSave(draft.toNote(rule)) },
                enabled = canSave && !saving,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(stringResource(if (saving) Res.string.reminder_saving else Res.string.reminder_save))
            }
        }
    }
}

@Composable
private fun Field(
    label: StringResource,
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    placeholder: StringResource? = null,
    multiline: Boolean = false,
    numeric: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },
        placeholder = placeholder?.let { hint -> { Text(stringResource(hint)) } },
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
