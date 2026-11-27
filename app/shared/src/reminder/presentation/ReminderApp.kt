package com.ygoular.keepit.reminder.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ygoular.keepit.design.theme.KeepItTheme
import com.ygoular.keepit.reminder.domain.ReminderNote
import com.ygoular.keepit.reminder.domain.ReminderStore
import com.ygoular.keepit.reminder.presentation.model.NoteDraft
import com.ygoular.keepit.reminder.presentation.screens.NoteEditorScreen
import com.ygoular.keepit.reminder.presentation.screens.NoteListScreen
import com.ygoular.keepit.resources.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@Composable
fun ReminderApp(
    store: ReminderStore,
    openedNoteId: String? = null,
    notificationStatus: String? = null,
    requestNotifications: (() -> Unit)? = null,
    systemBack: @Composable (enabled: Boolean, onBack: () -> Unit) -> Unit = { _, _ -> },
    openRequest: Int = 0,
) {
    var notes by remember(store) { mutableStateOf(emptyList<ReminderNote>()) }
    var draft by rememberSaveable(stateSaver = NoteDraft.saver) { mutableStateOf<NoteDraft?>(null) }
    var draftDirty by rememberSaveable { mutableStateOf(false) }
    var handledOpenRequest by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingOpenNoteId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmingDiscard by rememberSaveable { mutableStateOf(false) }
    var loading by remember(store) { mutableStateOf(true) }
    var saving by remember(store) { mutableStateOf(false) }
    var failedToLoad by remember(store) { mutableStateOf(false) }
    var failure by remember(store) { mutableStateOf<StringResource?>(null) }
    var reload by remember(store) { mutableStateOf(0) }
    var confirmingDelete by remember(store) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun showDraft(value: NoteDraft?) {
        draft = value
        draftDirty = false
        failure = null
    }

    LaunchedEffect(store, reload) {
        loading = true
        failedToLoad = false
        failure = null
        try {
            notes = store.list()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            failedToLoad = true
        } finally {
            loading = false
        }
    }
    LaunchedEffect(openedNoteId, openRequest, loading, failedToLoad, saving, confirmingDelete) {
        if (openedNoteId == null || loading || failedToLoad || saving || confirmingDelete) return@LaunchedEffect
        val request = "$openRequest:$openedNoteId"
        if (handledOpenRequest == request) return@LaunchedEffect
        handledOpenRequest = request
        if (draft?.id == openedNoteId) return@LaunchedEffect

        val opened = notes.find { it.id == openedNoteId }
        if (opened == null) {
            failure = Res.string.reminder_note_missing
        } else if (draftDirty) {
            pendingOpenNoteId = openedNoteId
            confirmingDiscard = true
        } else {
            showDraft(NoteDraft.fromNote(opened))
        }
    }

    val activeDraft = draft
    val now by produceState(Clock.System.now()) {
        while (true) {
            delay(1.minutes)
            value = Clock.System.now()
        }
    }
    val closeEditor = {
        if (draftDirty) confirmingDiscard = true else showDraft(null)
    }
    systemBack(activeDraft != null && !saving && !confirmingDelete && !confirmingDiscard, closeEditor)
    KeepItTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                    if (activeDraft != null) {
                        NoteEditorScreen(
                            draft = activeDraft,
                            originalRule = notes.firstOrNull { it.id == activeDraft.id }?.rule,
                            now = now,
                            saving = saving,
                            failure = failure,
                            onChange = {
                                if (!saving && it != draft) {
                                    draft = it
                                    draftDirty = true
                                    failure = null
                                }
                            },
                            onClose = closeEditor,
                            onDelete = { confirmingDelete = true },
                            onSave = { note ->
                                saving = true
                                scope.launch {
                                    try {
                                        store.save(note)
                                        notes = listOf(note) + notes.filterNot { it.id == note.id }
                                        showDraft(null)
                                    } catch (exception: CancellationException) {
                                        throw exception
                                    } catch (_: Exception) {
                                        failure = Res.string.reminder_save_failed
                                    } finally {
                                        saving = false
                                    }
                                }
                            },
                        )
                    } else {
                        NoteListScreen(
                            notes = notes,
                            now = now,
                            loading = loading,
                            failedToLoad = failedToLoad,
                            failure = failure,
                            notificationStatus = notificationStatus,
                            requestNotifications = requestNotifications,
                            onRetry = { reload++ },
                            onNew = { showDraft(NoteDraft.new()) },
                            onOpen = { showDraft(NoteDraft.fromNote(it)) },
                        )
                    }
                }
            }
        }

        val deletingId = activeDraft?.id
        if (confirmingDelete && deletingId != null) {
            AlertDialog(
                onDismissRequest = { confirmingDelete = false },
                title = { Text(stringResource(Res.string.reminder_delete_title)) },
                text = { Text(stringResource(Res.string.reminder_delete_message)) },
                dismissButton = {
                    TextButton(onClick = { confirmingDelete = false }) {
                        Text(stringResource(Res.string.reminder_cancel))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmingDelete = false
                        saving = true
                        scope.launch {
                            try {
                                store.delete(deletingId)
                                notes = notes.filterNot { it.id == deletingId }
                                showDraft(null)
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (_: Exception) {
                                failure = Res.string.reminder_delete_failed
                            } finally {
                                saving = false
                            }
                        }
                    }) {
                        Text(stringResource(Res.string.reminder_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
            )
        }

        if (confirmingDiscard) {
            val targetId = pendingOpenNoteId
            val keepEditing = {
                confirmingDiscard = false
                pendingOpenNoteId = null
            }
            AlertDialog(
                onDismissRequest = keepEditing,
                title = { Text(stringResource(Res.string.reminder_discard_title)) },
                text = { Text(stringResource(Res.string.reminder_discard_message)) },
                dismissButton = {
                    TextButton(onClick = keepEditing) { Text(stringResource(Res.string.reminder_keep_editing)) }
                },
                confirmButton = {
                    TextButton(
                        enabled = targetId == null || !loading,
                        onClick = {
                            keepEditing()
                            val linkedNote = notes.find { it.id == targetId }
                            if (targetId != null && linkedNote == null) {
                                failure = Res.string.reminder_note_missing
                            } else {
                                showDraft(linkedNote?.let(NoteDraft::fromNote))
                            }
                        },
                    ) {
                        Text(stringResource(if (targetId == null) Res.string.reminder_discard else Res.string.reminder_open_reminder))
                    }
                },
            )
        }
    }
}
