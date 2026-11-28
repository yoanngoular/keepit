package com.ygoular.keepit.reminder.storage

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL
import androidx.sqlite.async.prepare
import androidx.sqlite.async.step

private const val VERSION = 5

private fun createNotes(table: String) = """CREATE TABLE $table (
    id TEXT PRIMARY KEY NOT NULL,
    title TEXT NOT NULL, description TEXT NOT NULL,
    start_date TEXT NOT NULL, local_time TEXT NOT NULL, zone_id TEXT NOT NULL,
    interval_months INTEGER, ordinal INTEGER, weekday TEXT,
    end_date TEXT, revision TEXT NOT NULL, consumed INTEGER NOT NULL,
    rule_kind TEXT NOT NULL, interval_weeks INTEGER, weekdays TEXT,
    interval_days INTEGER, interval_years INTEGER, day_of_month INTEGER,
    CHECK (rule_kind IN ('monthly', 'weekly', 'once', 'daily', 'yearly', 'monthly_day')),
    CHECK ((rule_kind IN ('monthly', 'monthly_day')) = (interval_months IS NOT NULL)),
    CHECK ((rule_kind = 'monthly') = (ordinal IS NOT NULL)),
    CHECK ((rule_kind = 'monthly') = (weekday IS NOT NULL)),
    CHECK ((rule_kind = 'weekly') = (interval_weeks IS NOT NULL)),
    CHECK ((rule_kind = 'weekly') = (weekdays IS NOT NULL)),
    CHECK ((rule_kind = 'daily') = (interval_days IS NOT NULL)),
    CHECK ((rule_kind = 'yearly') = (interval_years IS NOT NULL)),
    CHECK ((rule_kind = 'monthly_day') = (day_of_month IS NOT NULL)),
    CHECK (rule_kind <> 'once' OR end_date IS NULL))"""

private suspend fun SQLiteConnection.rebuildNotes(version: Int) {
    executeSQL(createNotes("notes_v5"))
    copyLegacyNotes(version)
    executeSQL("DROP TABLE notes")
    executeSQL("ALTER TABLE notes_v5 RENAME TO notes")
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
            1, 2, 3, 4 -> rebuildNotes(current)
            else -> error("Missing KeepIt migration from $current to $VERSION")
        }
        executeSQL("PRAGMA user_version = $VERSION")
        executeSQL("COMMIT")
    } catch (failure: Throwable) {
        if (inTransaction()) executeSQL("ROLLBACK")
        throw failure
    }
}
