package com.ygoular.keepit.reminder.storage

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.async.executeSQL
import androidx.sqlite.async.open
import androidx.sqlite.async.prepare
import androidx.sqlite.async.step
import com.ygoular.keepit.reminder.domain.ReminderRule
import kotlin.coroutines.cancellation.CancellationException

private const val SELECT_STORED_NOTES = """SELECT n.id, n.title, n.description, $REMINDER_COLUMNS
    FROM notes n LEFT JOIN reminders r ON r.note_id = n.id AND r.id = n.id"""

class NoteDatabase(private val driver: SQLiteDriver, private val path: String) {
    private suspend inline fun <T> withDatabase(block: (SQLiteConnection) -> T): T =
        driver.open(path).use { connection ->
            connection.executeSQL("PRAGMA busy_timeout = 5000")
            connection.executeSQL("PRAGMA foreign_keys = ON")
            connection.ensureNoteSchema()
            block(connection)
        }

    suspend fun all(): List<StoredNote> = buildList {
        forEachNote({ add(it) }, { throw it })
    }

    suspend fun forEachNote(visit: suspend (StoredNote) -> Unit, onFailure: (RuntimeException) -> Unit) {
        withDatabase { connection ->
            connection.prepare("$SELECT_STORED_NOTES ORDER BY n.rowid DESC").use { statement ->
                while (statement.step()) {
                    try {
                        visit(statement.readStoredNote())
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: RuntimeException) {
                        onFailure(failure)
                    }
                }
            }
        }
    }

    suspend fun find(id: String): StoredNote? = withDatabase { connection ->
        connection.prepare("$SELECT_STORED_NOTES WHERE n.id = ?").use { statement ->
            statement.bindText(1, id)
            if (statement.step()) statement.readStoredNote() else null
        }
    }

    suspend fun save(row: StoredNote) = withDatabase { connection ->
        connection.writeTransaction {
            connection.prepare(
                """INSERT INTO notes (id, title, description) VALUES (?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET title = excluded.title, description = excluded.description"""
            ).use { statement ->
                statement.bindText(1, row.note.id)
                statement.bindText(2, row.note.title)
                statement.bindText(3, row.note.description)
                statement.step()
            }
            val rule = row.note.rule
            if (rule == null) {
                connection.removeReminder(row.note.id)
            } else {
                connection.saveReminder(row.note.id, rule, checkNotNull(row.revision), row.consumed)
            }
        }
    }

    suspend fun consume(id: String, number: Int) {
        withDatabase { connection ->
            connection.prepare("UPDATE reminders SET consumed = ? WHERE id = ? AND note_id = id AND consumed < ?").use { statement ->
                statement.bindInt(1, number)
                statement.bindText(2, id)
                statement.bindInt(3, number)
                statement.step()
            }
        }
    }

    suspend fun delete(id: String) {
        withDatabase { connection ->
            connection.writeTransaction {
                connection.prepare("DELETE FROM notes WHERE id = ?").use { statement ->
                    statement.bindText(1, id)
                    statement.step()
                }
            }
        }
    }
}

private suspend fun SQLiteConnection.removeReminder(id: String) {
    prepare("DELETE FROM reminders WHERE id = ? AND note_id = id").use { statement ->
        statement.bindText(1, id)
        statement.step()
    }
}

private suspend fun SQLiteConnection.saveReminder(id: String, rule: ReminderRule, revision: String, consumed: Int) {
    prepare(
        """INSERT INTO reminders (id, note_id, $REMINDER_COLUMNS) VALUES (?, ?, $REMINDER_VALUES)
            ON CONFLICT(id) DO UPDATE SET note_id = excluded.note_id,
            start_date = excluded.start_date, local_time = excluded.local_time, zone_id = excluded.zone_id,
            interval_months = excluded.interval_months, ordinal = excluded.ordinal,
            weekday = excluded.weekday, end_date = excluded.end_date,
            revision = excluded.revision, consumed = excluded.consumed,
            rule_kind = excluded.rule_kind, interval_weeks = excluded.interval_weeks, weekdays = excluded.weekdays,
            interval_days = excluded.interval_days, interval_years = excluded.interval_years,
            day_of_month = excluded.day_of_month"""
    ).use { statement ->
        // The first attachment retains the note ID so existing alarm targets continue to resolve.
        statement.bindText(1, id)
        statement.bindText(2, id)
        statement.bindReminderDetails(rule, revision, consumed, offset = 2)
        statement.step()
    }
}
