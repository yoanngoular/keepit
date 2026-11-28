package com.ygoular.keepit.reminder.platform

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ygoular.keepit.reminder.domain.ReminderDeliveryPolicy
import com.ygoular.keepit.reminder.domain.ReminderDeliveryPolicy.AlarmDecision
import com.ygoular.keepit.reminder.domain.ReminderNote
import com.ygoular.keepit.reminder.domain.ReminderStore
import com.ygoular.keepit.reminder.domain.isPastOneTimeChange
import com.ygoular.keepit.reminder.storage.NoteDatabase
import com.ygoular.keepit.reminder.storage.StoredNote
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.time.Clock

internal class DeviceReminders private constructor(context: Context) : ReminderStore {
    companion object {
        @Volatile private var instance: DeviceReminders? = null
        fun get(context: Context): DeviceReminders = instance ?: synchronized(this) {
            instance ?: DeviceReminders(context.applicationContext).also { instance = it }
        }
    }

    private val database by lazy {
        val file = context.getDatabasePath("keepit.db")
        val directory = checkNotNull(file.parentFile)
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create the notes database directory" }
        NoteDatabase(BundledSQLiteDriver(), file.absolutePath)
    }
    private val deliveryLock = Mutex()
    val alarms = AlarmScheduler(context)
    val notifications = ReminderNotifications(context)

    private val schedulingFailed = MutableStateFlow(false)
    val schedulerUnavailable = schedulingFailed.asStateFlow()

    private inline fun guardScheduling(block: () -> Unit) {
        try {
            block()
        } catch (failure: RuntimeException) {
            reportFailure(failure)
        }
    }

    private fun reportFailure(failure: RuntimeException) {
        if (failure is CancellationException) throw failure
        schedulingFailed.value = true
        Log.e("KeepItReminders", "Could not reconcile reminder delivery (${failure.javaClass.simpleName})")
    }

    override suspend fun list() = withContext(Dispatchers.IO) {
        deliveryLock.withLock { database.all().map { it.note } }
    }

    override suspend fun save(note: ReminderNote) = withContext(Dispatchers.IO) {
        deliveryLock.withLock {
            val previous = database.find(note.id)
            val rule = note.rule
            val row = when {
                rule == null -> StoredNote(note, revision = null, consumed = 0)
                previous?.note?.rule == rule -> previous.copy(note = note)
                else -> {
                    val now = Clock.System.now()
                    require(!rule.isPastOneTimeChange(previous?.note?.rule, now)) {
                        "A new or changed one-time reminder must be in the future."
                    }
                    StoredNote(note, UUID.randomUUID().toString(), ReminderDeliveryPolicy.initialConsumed(rule, now))
                }
            }
            database.save(row)
            if (rule != null) {
                guardScheduling { schedule(row) }
            } else if (previous?.note?.rule != null) {
                cancelReminder(note.id)
            }
        }
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        deliveryLock.withLock {
            database.delete(id)
            cancelReminder(id)
        }
    }

    private fun cancelReminder(id: String) {
        guardScheduling { alarms.cancel(id) }
        guardScheduling { notifications.cancel(id) }
    }

    suspend fun reconcile() = withContext(Dispatchers.IO) {
        deliveryLock.withLock {
            guardScheduling {
                var allScheduled = true
                database.forEachNote(::schedule) { failure ->
                    allScheduled = false
                    reportFailure(failure)
                }
                schedulingFailed.value = !allScheduled
            }
        }
    }

    private fun schedule(row: StoredNote) {
        val rule = row.note.rule ?: return
        val revision = checkNotNull(row.revision)
        val occurrence = ReminderDeliveryPolicy.next(rule, row.consumed, Clock.System.now())
            ?: return alarms.cancel(row.note.id)
        alarms.schedule(row.note.id, revision, occurrence)
    }

    suspend fun receive(intent: Intent) = withContext(Dispatchers.IO) {
        deliveryLock.withLock { guardScheduling { deliver(intent) } }
    }

    private suspend fun deliver(intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION) return
        val id = ReminderRoute.noteId(intent.data) ?: return
        val row = database.find(id) ?: return
        val rule = row.note.rule ?: return
        val revision = row.revision ?: return
        when (val decision = ReminderDeliveryPolicy.onAlarm(
            rule = rule,
            consumed = row.consumed,
            now = Clock.System.now(),
            revision = revision,
            alarmRevision = intent.getStringExtra(AlarmScheduler.REVISION),
            alarmNumber = intent.getIntExtra(AlarmScheduler.NUMBER, 0),
        )) {
            AlarmDecision.Ignore -> return
            is AlarmDecision.Rearm -> alarms.schedule(id, revision, decision.occurrence)
            is AlarmDecision.Deliver -> {
                // A failed or disabled notification consumes this occurrence. Future occurrences still run.
                guardScheduling { notifications.show(row.note) }
                database.consume(id, decision.occurrence.number)
                schedule(row.copy(consumed = decision.occurrence.number))
            }
        }
    }
}
