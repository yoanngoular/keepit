package com.ygoular.keepit.reminder.storage

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.async.executeSQL
import androidx.sqlite.async.open
import androidx.sqlite.async.prepare
import androidx.sqlite.async.step
import kotlin.coroutines.cancellation.CancellationException

class NoteDatabase(private val driver: SQLiteDriver, private val path: String) {
    private suspend inline fun <T> withDatabase(block: (SQLiteConnection) -> T): T =
        driver.open(path).use { connection ->
            connection.executeSQL("PRAGMA busy_timeout = 5000")
            connection.ensureNoteSchema()
            block(connection)
        }

    suspend fun all(): List<StoredNote> = buildList {
        forEachNote({ add(it) }, { throw it })
    }

    suspend fun forEachNote(visit: suspend (StoredNote) -> Unit, onFailure: (RuntimeException) -> Unit) {
        withDatabase { connection ->
            connection.prepare("SELECT $NOTE_COLUMNS FROM notes ORDER BY rowid DESC").use { statement ->
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
        connection.prepare("SELECT $NOTE_COLUMNS FROM notes WHERE id = ?").use { statement ->
            statement.bindText(1, id)
            if (statement.step()) statement.readStoredNote() else null
        }
    }

    suspend fun save(row: StoredNote) {
        withDatabase { connection ->
            connection.prepare(
                """INSERT INTO notes ($NOTE_COLUMNS) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(id) DO UPDATE SET
                    title = excluded.title, description = excluded.description,
                    start_date = excluded.start_date, local_time = excluded.local_time, zone_id = excluded.zone_id,
                    interval_months = excluded.interval_months, ordinal = excluded.ordinal,
                    weekday = excluded.weekday, occurrence_count = excluded.occurrence_count,
                    revision = excluded.revision, consumed = excluded.consumed,
                    rule_kind = excluded.rule_kind, interval_weeks = excluded.interval_weeks, weekdays = excluded.weekdays"""
            ).use { statement ->
                statement.bindStoredNote(row)
                statement.step()
            }
        }
    }

    suspend fun consume(id: String, number: Int) {
        withDatabase { connection ->
            connection.prepare("UPDATE notes SET consumed = ? WHERE id = ? AND consumed < ?").use { statement ->
                statement.bindInt(1, number)
                statement.bindText(2, id)
                statement.bindInt(3, number)
                statement.step()
            }
        }
    }

    suspend fun delete(id: String) {
        withDatabase { connection ->
            connection.prepare("DELETE FROM notes WHERE id = ?").use { statement ->
                statement.bindText(1, id)
                statement.step()
            }
        }
    }
}
