package com.ygoular.keepit.reminder.storage

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.ygoular.keepit.reminder.domain.MonthlyReminderRule
import com.ygoular.keepit.reminder.domain.ReminderNote
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NoteDatabaseTest {
    private val driver = BundledSQLiteDriver()
    private val rule = MonthlyReminderRule(
        LocalDate(2026, 11, 1), LocalTime(8, 30), "Europe/Paris", 2, 3, DayOfWeek.TUESDAY, 9,
    )

    private fun row(id: String) = StoredNote(ReminderNote(id, "Title", "Body", rule), "revision", 0)

    private fun onDisk(block: suspend (String) -> Unit) {
        val file = Files.createTempFile("keepit-storage-", ".db")
        try {
            runBlocking { block(file.toString()) }
        } finally {
            Files.delete(file)
        }
    }

    @Test
    fun editsPreserveOrderAndProgressAcrossReopens() = onDisk { path ->
        val db = NoteDatabase(driver, path)
        db.save(row("first"))
        db.save(row("second"))
        val edited = row("first").copy(note = row("first").note.copy(title = "Updated"))
        db.save(edited)
        db.consume("first", 2)
        db.consume("first", 1)
        val reopened = NoteDatabase(driver, path)
        assertEquals(listOf(row("second"), edited.copy(consumed = 2)), reopened.all())
        reopened.delete("second")
        assertNull(db.find("second"))
        assertEquals(edited.copy(consumed = 2), db.find("first"))
    }

    @Test
    fun readsKeepItVersionOneIncludingPreviouslyConsumedOccurrences() = onDisk { path ->
        driver.open(path).use { connection ->
            connection.execSQL("""CREATE TABLE notes (
                id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL,
                start_date TEXT NOT NULL, local_time TEXT NOT NULL, zone_id TEXT NOT NULL,
                interval_months INTEGER NOT NULL, ordinal INTEGER NOT NULL, weekday TEXT NOT NULL,
                occurrence_count INTEGER NOT NULL, revision TEXT NOT NULL, consumed INTEGER NOT NULL)""")
            connection.execSQL("""INSERT INTO notes VALUES (
                'one', 'Title', 'Body', '2026-11-01', '08:30', 'Europe/Paris', 2, 3, 'TUESDAY', 9, 'revision', 4)""")
            connection.execSQL("PRAGMA user_version=1")
        }
        assertEquals(row("one").copy(consumed = 4), NoteDatabase(driver, path).find("one"))
    }

    @Test
    fun invalidRecordCanBeReportedWithoutBlockingOtherReminders() = onDisk { path ->
        val db = NoteDatabase(driver, path)
        db.save(row("valid"))
        db.save(row("broken"))
        driver.open(path).use { it.execSQL("UPDATE notes SET start_date = 'invalid' WHERE id = 'broken'") }
        val delivered = mutableListOf<String>()
        val failures = mutableListOf<RuntimeException>()
        db.forEachNote({ delivered.add(it.note.id) }, { failures.add(it) })
        assertEquals(listOf("valid"), delivered)
        assertEquals(1, failures.size)
    }

    @Test
    fun unsupportedVersionLeavesRowsAndVersionIntact() = onDisk { path ->
        val db = NoteDatabase(driver, path)
        db.save(row("one"))
        driver.open(path).use { it.execSQL("PRAGMA user_version=42") }
        assertFailsWith<IllegalStateException> { db.delete("one") }
        driver.open(path).use { connection ->
            connection.prepare("PRAGMA user_version").use { statement ->
                statement.step()
                assertEquals(42, statement.getInt(0))
            }
            connection.prepare("SELECT id FROM notes").use { statement ->
                statement.step()
                assertEquals("one", statement.getText(0))
            }
        }
    }
}
