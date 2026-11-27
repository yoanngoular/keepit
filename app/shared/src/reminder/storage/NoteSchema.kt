package com.ygoular.keepit.reminder.storage

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL
import androidx.sqlite.async.prepare
import androidx.sqlite.async.step

private const val VERSION = 3

private fun createNotes(table: String) = """CREATE TABLE $table (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL, description TEXT NOT NULL,
    start_date TEXT NOT NULL, local_time TEXT NOT NULL, zone_id TEXT NOT NULL,
    interval_months INTEGER, ordinal INTEGER, weekday TEXT,
    occurrence_count INTEGER NOT NULL, revision TEXT NOT NULL, consumed INTEGER NOT NULL,
    rule_kind TEXT NOT NULL, interval_weeks INTEGER, weekdays TEXT,
    CHECK (
        (rule_kind = 'monthly' AND interval_months IS NOT NULL AND ordinal IS NOT NULL AND weekday IS NOT NULL
            AND interval_weeks IS NULL AND weekdays IS NULL)
        OR
        (rule_kind = 'weekly' AND interval_months IS NULL AND ordinal IS NULL AND weekday IS NULL
            AND interval_weeks IS NOT NULL AND weekdays IS NOT NULL)
        OR
        (rule_kind = 'once' AND occurrence_count = 1 AND interval_months IS NULL AND ordinal IS NULL
            AND weekday IS NULL AND interval_weeks IS NULL AND weekdays IS NULL)
    ))"""

private suspend fun SQLiteConnection.migrateFromVersionOne() = rebuildNotes(
    """SELECT rowid, id, title, description, start_date, local_time, zone_id,
        interval_months, ordinal, weekday, occurrence_count, revision, consumed,
        'monthly', NULL, NULL FROM notes"""
)

private suspend fun SQLiteConnection.migrateFromVersionTwo() =
    rebuildNotes("SELECT rowid, $NOTE_COLUMNS FROM notes")

private suspend fun SQLiteConnection.rebuildNotes(select: String) {
    executeSQL(createNotes("notes_v3"))
    executeSQL("INSERT INTO notes_v3 (rowid, $NOTE_COLUMNS) $select")
    executeSQL("DROP TABLE notes")
    executeSQL("ALTER TABLE notes_v3 RENAME TO notes")
}

private suspend fun SQLiteConnection.version(): Int =
    prepare("PRAGMA user_version").use { statement ->
        check(statement.step())
        statement.getInt(0)
    }

internal suspend fun SQLiteConnection.ensureNoteSchema() {
    if (version() == VERSION) return
    executeSQL("BEGIN IMMEDIATE")
    try {
        when (val current = version()) {
            // Another connection may have migrated the schema while we waited for the lock.
            VERSION -> Unit
            0 -> executeSQL(createNotes("notes"))
            1 -> migrateFromVersionOne()
            2 -> migrateFromVersionTwo()
            else -> error("Missing KeepIt migration from $current to $VERSION")
        }
        executeSQL("PRAGMA user_version = $VERSION")
        executeSQL("COMMIT")
    } catch (failure: Throwable) {
        if (inTransaction()) executeSQL("ROLLBACK")
        throw failure
    }
}
