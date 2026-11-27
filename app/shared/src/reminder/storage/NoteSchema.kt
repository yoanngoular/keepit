package com.ygoular.keepit.reminder.storage

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL
import androidx.sqlite.async.prepare
import androidx.sqlite.async.step

private const val VERSION = 1

private const val CREATE_NOTES = """CREATE TABLE notes (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL, description TEXT NOT NULL,
    start_date TEXT NOT NULL, local_time TEXT NOT NULL, zone_id TEXT NOT NULL,
    interval_months INTEGER NOT NULL, ordinal INTEGER NOT NULL,
    weekday TEXT NOT NULL, occurrence_count INTEGER NOT NULL,
    revision TEXT NOT NULL, consumed INTEGER NOT NULL)"""

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
            // Another connection may have created the schema while we waited for the lock.
            VERSION -> Unit
            0 -> {
                executeSQL(CREATE_NOTES)
                executeSQL("PRAGMA user_version = $VERSION")
            }
            else -> error("Missing KeepIt migration from $current to $VERSION")
        }
        executeSQL("COMMIT")
    } catch (failure: Throwable) {
        if (inTransaction()) executeSQL("ROLLBACK")
        throw failure
    }
}
