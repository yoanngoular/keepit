package com.ygoular.keepit.reminder.storage

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.ygoular.keepit.reminder.domain.DailyReminderRule
import com.ygoular.keepit.reminder.domain.MonthlyDayReminderRule
import com.ygoular.keepit.reminder.domain.MonthlyReminderRule
import com.ygoular.keepit.reminder.domain.OneTimeReminderRule
import com.ygoular.keepit.reminder.domain.ReminderNote
import com.ygoular.keepit.reminder.domain.WeeklyReminderRule
import com.ygoular.keepit.reminder.domain.YearlyReminderRule
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
        LocalDate(2026, 11, 1), LocalTime(8, 30), "Europe/Paris", 2, 3, DayOfWeek.TUESDAY, LocalDate(2028, 3, 21),
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

    private fun createVersionOne(path: String) {
        driver.open(path).use { connection ->
            connection.execSQL("""CREATE TABLE notes (
                id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL,
                start_date TEXT NOT NULL, local_time TEXT NOT NULL, zone_id TEXT NOT NULL,
                interval_months INTEGER NOT NULL, ordinal INTEGER NOT NULL, weekday TEXT NOT NULL,
                occurrence_count INTEGER NOT NULL, revision TEXT NOT NULL, consumed INTEGER NOT NULL)""")
            connection.execSQL("""INSERT INTO notes VALUES (
                'older', 'Title', 'Body', '2026-11-01', '08:30', 'Europe/Paris', 2, 3, 'TUESDAY', 9, 'revision', 4)""")
            connection.execSQL("""INSERT INTO notes VALUES (
                'newer', 'Latest', 'Description', '2026-12-15', '19:45', 'UTC', 5, 4, 'FRIDAY', 12, 'newer-revision', 2)""")
            connection.execSQL("UPDATE notes SET rowid = 12 WHERE id = 'newer'")
            connection.execSQL("PRAGMA user_version = 1")
        }
    }

    @Test
    fun upgradesVersionOnePreservingNotesRuleFieldsProgressAndRowIds() = onDisk { path ->
        createVersionOne(path)
        val older = row("older").copy(consumed = 4)
        val newerRule = MonthlyReminderRule(
            LocalDate(2026, 12, 15), LocalTime(19, 45), "UTC", 5, 4, DayOfWeek.FRIDAY, LocalDate(2031, 7, 25),
        )
        val newer = StoredNote(ReminderNote("newer", "Latest", "Description", newerRule), "newer-revision", 2)

        assertEquals(listOf(newer, older), NoteDatabase(driver, path).all())
        driver.open(path).use { connection ->
            connection.prepare("PRAGMA user_version").use { statement ->
                statement.step()
                assertEquals(6, statement.getInt(0))
            }
            connection.prepare("SELECT rowid FROM notes WHERE id = 'newer'").use { statement ->
                statement.step()
                assertEquals(12L, statement.getLong(0))
            }
        }
    }

    private fun createVersionTwoOrThree(path: String, version: Int) {
        val onceConstraint = if (version == 3) {
            """OR (rule_kind = 'once' AND occurrence_count = 1 AND interval_months IS NULL AND ordinal IS NULL
                AND weekday IS NULL AND interval_weeks IS NULL AND weekdays IS NULL)"""
        } else ""
        driver.open(path).use { connection ->
            connection.execSQL("""CREATE TABLE notes (
                id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL,
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
                    $onceConstraint
                ))""")
            connection.execSQL("""INSERT INTO notes VALUES (
                'monthly', 'Title', 'Body', '2026-11-01', '08:30', 'Europe/Paris', 2, 3, 'TUESDAY', 9, 'revision', 4,
                'monthly', NULL, NULL)""")
            connection.execSQL("""INSERT INTO notes VALUES (
                'weekly', 'Weekly title', 'Weekly body', '2026-11-25', '08:30', 'Europe/Paris', NULL, NULL, NULL,
                9, 'weekly-revision', 2, 'weekly', 2, 'MONDAY,FRIDAY')""")
            connection.execSQL("UPDATE notes SET rowid = 21 WHERE id = 'weekly'")
            if (version == 3) {
                connection.execSQL("""UPDATE notes
                    SET start_date = '2011-11-25', zone_id = 'Pacific/Apia', occurrence_count = 6,
                        interval_weeks = 1, weekdays = 'FRIDAY'
                    WHERE id = 'weekly'""")
                connection.execSQL("""INSERT INTO notes VALUES (
                    'once', 'One title', 'One body', '2026-11-28', '12:30', 'UTC', NULL, NULL, NULL,
                    1, 'once-revision', 1, 'once', NULL, NULL)""")
            }
            connection.execSQL("PRAGMA user_version = $version")
        }
    }

    @Test
    fun upgradesVersionTwoPreservingMonthlyAndWeeklyRules() = onDisk { path ->
        createVersionTwoOrThree(path, 2)
        val weeklyRule = WeeklyReminderRule(
            LocalDate(2026, 11, 25), LocalTime(8, 30), "Europe/Paris", 2,
            setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), LocalDate(2027, 1, 22),
        )
        val weekly = StoredNote(ReminderNote("weekly", "Weekly title", "Weekly body", weeklyRule), "weekly-revision", 2)

        assertEquals(listOf(weekly, row("monthly").copy(consumed = 4)), NoteDatabase(driver, path).all())
        driver.open(path).use { connection ->
            connection.prepare("PRAGMA user_version").use { statement ->
                statement.step()
                assertEquals(6, statement.getInt(0))
            }
            connection.prepare("SELECT rowid FROM notes WHERE id = 'weekly'").use { statement ->
                statement.step()
                assertEquals(21L, statement.getLong(0))
            }
        }
    }

    @Test
    fun upgradesVersionThreePreservingOneTimeAndSixthCivilWeeklyDate() = onDisk { path ->
        createVersionTwoOrThree(path, 3)
        val weeklyRule = WeeklyReminderRule(
            LocalDate(2011, 11, 25), LocalTime(8, 30), "Pacific/Apia", 1,
            setOf(DayOfWeek.FRIDAY), LocalDate(2011, 12, 30),
        )
        val weekly = StoredNote(ReminderNote("weekly", "Weekly title", "Weekly body", weeklyRule), "weekly-revision", 2)
        val onceRule = OneTimeReminderRule(LocalDate(2026, 11, 28), LocalTime(12, 30), "UTC")
        val once = StoredNote(ReminderNote("once", "One title", "One body", onceRule), "once-revision", 1)

        // Apia skipped the last occurrence's civil date. The migrated limit still uses that date.
        assertEquals(listOf(once, weekly, row("monthly").copy(consumed = 4)), NoteDatabase(driver, path).all())
    }

    @Test
    fun upgradesVersionFourPreservingExistingRuleValuesAndAlarmState() = onDisk { path ->
        driver.open(path).use { connection ->
            connection.execSQL("""CREATE TABLE notes (
                id TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL,
                start_date TEXT NOT NULL, local_time TEXT NOT NULL, zone_id TEXT NOT NULL,
                interval_months INTEGER, ordinal INTEGER, weekday TEXT,
                end_date TEXT, revision TEXT NOT NULL, consumed INTEGER NOT NULL,
                rule_kind TEXT NOT NULL, interval_weeks INTEGER, weekdays TEXT,
                CHECK (
                    (rule_kind = 'monthly' AND interval_months IS NOT NULL AND ordinal IS NOT NULL AND weekday IS NOT NULL
                        AND interval_weeks IS NULL AND weekdays IS NULL)
                    OR
                    (rule_kind = 'weekly' AND interval_months IS NULL AND ordinal IS NULL AND weekday IS NULL
                        AND interval_weeks IS NOT NULL AND weekdays IS NOT NULL)
                    OR
                    (rule_kind = 'once' AND end_date IS NULL AND interval_months IS NULL AND ordinal IS NULL
                        AND weekday IS NULL AND interval_weeks IS NULL AND weekdays IS NULL)
                ))""")
            connection.execSQL("""INSERT INTO notes VALUES (
                'monthly', 'Title', 'Body', '2026-11-01', '08:30', 'Europe/Paris', 2, 3, 'TUESDAY',
                '2028-03-21', 'revision', 4, 'monthly', NULL, NULL),
                ('weekly', 'Weekly', 'Body', '2026-11-28', '10:15', 'UTC', NULL, NULL, NULL,
                NULL, 'weekly-revision', 2, 'weekly', 3, 'MONDAY,FRIDAY'),
                ('once', 'Once', 'Body', '2026-11-29', '12:30', 'UTC', NULL, NULL, NULL,
                NULL, 'once-revision', 1, 'once', NULL, NULL)""")
            connection.execSQL("UPDATE notes SET rowid = 32 WHERE id = 'once'")
            connection.execSQL("PRAGMA user_version = 4")
        }
        val weeklyRule = WeeklyReminderRule(
            LocalDate(2026, 11, 28), LocalTime(10, 15), "UTC", 3,
            setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), null,
        )
        val weekly = StoredNote(ReminderNote("weekly", "Weekly", "Body", weeklyRule), "weekly-revision", 2)
        val onceRule = OneTimeReminderRule(LocalDate(2026, 11, 29), LocalTime(12, 30), "UTC")
        val once = StoredNote(ReminderNote("once", "Once", "Body", onceRule), "once-revision", 1)

        assertEquals(listOf(once, weekly, row("monthly").copy(consumed = 4)), NoteDatabase(driver, path).all())
        driver.open(path).use { connection ->
            connection.prepare("SELECT rowid FROM notes WHERE id = 'once'").use { statement ->
                statement.step()
                assertEquals(32L, statement.getLong(0))
            }
        }
    }

    @Test
    fun normalizesVersionFivePreservingNoteRowsAndAlarmState() = onDisk { path ->
        driver.open(path).use { connection ->
            connection.execSQL("""CREATE TABLE notes (
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
                CHECK (rule_kind <> 'once' OR end_date IS NULL))""")
            connection.execSQL("""INSERT INTO notes VALUES (
                'monthly', 'Title', 'Body', '2026-11-01', '08:30', 'Europe/Paris', 2, 3, 'TUESDAY',
                '2028-03-21', 'revision', 4, 'monthly', NULL, NULL, NULL, NULL, NULL),
                ('daily', 'Daily', 'Plan', '2026-11-28', '14:00', 'UTC', NULL, NULL, NULL,
                NULL, 'daily-revision', 12, 'daily', NULL, NULL, 5, NULL, NULL)""")
            connection.execSQL("UPDATE notes SET rowid = 40 WHERE id = 'daily'")
            connection.execSQL("PRAGMA user_version = 5")
        }
        val dailyRule = DailyReminderRule(LocalDate(2026, 11, 28), LocalTime(14, 0), "UTC", 5, null)
        val daily = StoredNote(ReminderNote("daily", "Daily", "Plan", dailyRule), "daily-revision", 12)

        assertEquals(listOf(daily, row("monthly").copy(consumed = 4)), NoteDatabase(driver, path).all())
        driver.open(path).use { connection ->
            connection.prepare("PRAGMA user_version").use { statement ->
                statement.step()
                assertEquals(6, statement.getInt(0))
            }
            connection.prepare("SELECT n.rowid, r.id, r.note_id FROM notes n JOIN reminders r ON r.note_id = n.id WHERE n.id = 'daily'")
                .use { statement ->
                    check(statement.step())
                    assertEquals(40L, statement.getLong(0))
                    assertEquals("daily", statement.getText(1))
                    assertEquals("daily", statement.getText(2))
                }
        }
    }

    @Test
    fun plainNotesCanAttachRemoveAndReattachAReminder() = onDisk { path ->
        val db = NoteDatabase(driver, path)
        val plain = StoredNote(ReminderNote("plain", "Title", "Body", null), null, 0)

        db.save(plain)
        db.consume("plain", 1)
        assertEquals(plain, NoteDatabase(driver, path).find("plain"))
        assertEquals(0, reminderCount(path))

        val firstAttachment = plain.copy(note = plain.note.copy(rule = rule), revision = "first")
        db.save(firstAttachment)
        db.consume("plain", 2)
        assertEquals(firstAttachment.copy(consumed = 2), db.find("plain"))
        assertEquals(1, reminderCount(path))

        val edited = plain.copy(note = plain.note.copy(description = "Still here"))
        db.save(edited)
        assertEquals(listOf(edited), NoteDatabase(driver, path).all())
        assertEquals(0, reminderCount(path))

        val secondAttachment = edited.copy(note = edited.note.copy(rule = rule), revision = "second")
        db.save(secondAttachment)
        assertEquals(secondAttachment, db.find("plain"))
        db.delete("plain")
        assertNull(db.find("plain"))
        assertEquals(0, reminderCount(path))
    }

    @Test
    fun failedAttachmentRollsBackNoteEdit() = onDisk { path ->
        val db = NoteDatabase(driver, path)
        val plain = StoredNote(ReminderNote("plain", "Original title", "Body", null), null, 0)
        db.save(plain)
        driver.open(path).use { connection ->
            connection.execSQL("""CREATE TRIGGER reject_reminder BEFORE INSERT ON reminders
                BEGIN SELECT RAISE(ABORT, 'Test failure'); END""")
        }
        val edited = StoredNote(plain.note.copy(title = "Edited title", rule = rule), "revision", 0)
        assertFailsWith<RuntimeException> { db.save(edited) }

        assertEquals(plain, NoteDatabase(driver, path).find("plain"))
        assertEquals(0, reminderCount(path))
    }

    private fun reminderCount(path: String): Int = driver.open(path).use { connection ->
        connection.prepare("SELECT count(*) FROM reminders").use { statement ->
            check(statement.step())
            statement.getInt(0)
        }
    }

    @Test
    fun roundTripsNewCadencesAndLastWeekdayAcrossReopens() = onDisk { path ->
        val start = LocalDate(2026, 11, 28)
        val time = LocalTime(8, 30)
        val through = LocalDate(2028, 11, 28)
        val storedRules = listOf(
            DailyReminderRule(start, time, "UTC", 7, through),
            YearlyReminderRule(start, time, "Europe/Paris", 2, null),
            MonthlyDayReminderRule(start, time, "UTC", 3, 31, through),
            MonthlyReminderRule(start, time, "UTC", 2, -1, DayOfWeek.FRIDAY, through),
        )
        val rows = storedRules.mapIndexed { index, rule ->
            StoredNote(ReminderNote(index.toString(), "Title", "Body", rule), "cadence-$index", 0)
        }
        val db = NoteDatabase(driver, path)
        rows.forEach { db.save(it) }
        assertEquals(rows.reversed(), NoteDatabase(driver, path).all())

        // Replacing a daily rule with a different cadence clears unused fields in the existing row.
        val replacement = rows[0].copy(note = rows[0].note.copy(rule = storedRules[2]))
        db.save(replacement)
        assertEquals(replacement, NoteDatabase(driver, path).find("0"))
        driver.open(path).use { connection ->
            assertFailsWith<RuntimeException> {
                connection.execSQL("UPDATE reminders SET interval_days = 7 WHERE id = '0'")
            }
        }
    }

    @Test
    fun unlimitedMonthlyAndWeeklyRulesRoundTripWithNullEndDates() = onDisk { path ->
        val monthly = row("monthly").let { it.copy(note = it.note.copy(rule = rule.copy(endDate = null))) }
        val weeklyRule = WeeklyReminderRule(
            LocalDate(2026, 11, 25), LocalTime(8, 30), "Europe/Paris", 2,
            setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), null,
        )
        val weekly = StoredNote(ReminderNote("weekly", "Weekly title", "Weekly body", weeklyRule), "weekly-revision", 2)
        val db = NoteDatabase(driver, path)
        db.save(monthly)
        db.save(weekly)
        assertEquals(listOf(weekly, monthly), NoteDatabase(driver, path).all())
    }

    @Test
    fun oneTimeRuleRoundTripsWithConsumedState() = onDisk { path ->
        val db = NoteDatabase(driver, path)
        val monthly = row("one")
        val rule = OneTimeReminderRule(LocalDate(2026, 11, 25), LocalTime(8, 30), "Europe/Paris")
        val oneTime = StoredNote(monthly.note.copy(rule = rule), "one-time-revision", 0)
        db.save(monthly)
        db.save(oneTime)
        assertEquals(oneTime, NoteDatabase(driver, path).find("one"))
        db.consume("one", 1)
        assertEquals(oneTime.copy(consumed = 1), NoteDatabase(driver, path).find("one"))
        db.save(monthly)
        assertEquals(monthly, db.find("one"))
    }

    @Test
    fun weeklyRuleRoundTripsAndCanReplaceAMonthlyRule() = onDisk { path ->
        val db = NoteDatabase(driver, path)
        val monthly = row("first")
        val rule = WeeklyReminderRule(
            LocalDate(2026, 11, 25), LocalTime(8, 30), "Europe/Paris", 2,
            setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), LocalDate(2027, 1, 22),
        )
        val weekly = StoredNote(monthly.note.copy(rule = rule), "weekly-revision", 2)
        db.save(monthly)
        db.save(row("second"))
        db.save(weekly)
        val reopened = NoteDatabase(driver, path)
        assertEquals(listOf(row("second"), weekly), reopened.all())
        reopened.save(monthly)
        assertEquals(monthly, db.find("first"))
    }

    @Test
    fun failedVersionOneMigrationLeavesNotesAndVersionIntact() = onDisk { path ->
        createVersionOne(path)
        driver.open(path).use { it.execSQL("UPDATE notes SET occurrence_count = 0 WHERE id = 'newer'") }
        assertFailsWith<RuntimeException> { NoteDatabase(driver, path).all() }
        driver.open(path).use { connection ->
            connection.prepare("PRAGMA user_version").use { statement ->
                statement.step()
                assertEquals(1, statement.getInt(0))
            }
            connection.prepare("SELECT title FROM notes WHERE id = 'newer'").use { statement ->
                statement.step()
                assertEquals("Latest", statement.getText(0))
            }
            connection.prepare("SELECT count(*) FROM sqlite_master WHERE name = 'notes_v5'").use { statement ->
                statement.step()
                assertEquals(0, statement.getInt(0))
            }
        }
    }


    @Test
    fun invalidRecordCanBeReportedWithoutBlockingOtherReminders() = onDisk { path ->
        val db = NoteDatabase(driver, path)
        db.save(row("valid"))
        db.save(row("broken"))
        driver.open(path).use { it.execSQL("UPDATE reminders SET start_date = 'invalid' WHERE id = 'broken'") }
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
