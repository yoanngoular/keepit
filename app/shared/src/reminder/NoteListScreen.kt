package com.ygoular.keepit.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ygoular.keepit.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
internal fun NoteListScreen(
    notes: List<ReminderNote>,
    now: Instant,
    loading: Boolean,
    failedToLoad: Boolean,
    failure: StringResource?,
    notificationStatus: String?,
    requestNotifications: (() -> Unit)?,
    onRetry: () -> Unit,
    onNew: () -> Unit,
    onOpen: (ReminderNote) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Headline(stringResource(Res.string.reminder_notes), Modifier.weight(1f))
                Button(onClick = onNew, enabled = !loading && !failedToLoad) {
                    Text(stringResource(Res.string.reminder_new_note))
                }
            }
        }
        if (notificationStatus != null) item {
            Panel(tinted = true) {
                Text(notificationStatus, style = MaterialTheme.typography.bodyMedium)
                if (requestNotifications != null) {
                    OutlinedButton(onClick = requestNotifications) {
                        Text(stringResource(Res.string.reminder_enable_notifications))
                    }
                }
            }
        }
        if (failure != null) item { ErrorText(failure) }
        when {
            loading -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            failedToLoad -> item {
                Panel {
                    ErrorText(Res.string.reminder_load_failed)
                    OutlinedButton(onClick = onRetry) { Text(stringResource(Res.string.reminder_retry)) }
                }
            }
            notes.isEmpty() -> item {
                Panel(tinted = true) {
                    Text(stringResource(Res.string.reminder_empty_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(Res.string.reminder_empty_body), style = MaterialTheme.typography.bodyMedium)
                }
            }
            else -> items(notes, key = { it.id }) { note ->
                val occurrences = remember(note.rule) { note.rule.occurrences() }
                val next = occurrences.firstOrNull { it.at >= now }
                Surface(
                    onClick = { onOpen(note) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            note.title.ifBlank { note.description },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (note.title.isNotBlank() && note.description.isNotBlank()) {
                            Text(note.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        val detail = if (next == null) {
                            stringResource(Res.string.reminder_no_upcoming)
                        } else {
                            stringResource(Res.string.reminder_next, occurrenceLabel(next, note.rule))
                        }
                        Text(
                            detail,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
        item { Box(Modifier.padding(bottom = 16.dp)) }
    }
}
