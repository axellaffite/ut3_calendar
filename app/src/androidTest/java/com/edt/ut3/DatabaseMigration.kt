package com.edt.ut3

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase.CONFLICT_NONE
import androidx.core.database.getStringOrNull
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.edt.ut3.refactored.models.repositories.database.AppDatabase
import org.json.JSONArray
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.*

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val TEST_DB = "migration-test"


    val MIGRATION_1_2 = object:  Migration(1,2) {
        override fun migrate(database: SupportSQLiteDatabase) {


            database.execSQL("CREATE TABLE IF NOT EXISTS `note_new` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`event_id` TEXT, " +
                    "`title` TEXT, " +
                    "`contents` TEXT NOT NULL, " +
                    "`date` INTEGER NOT NULL, " +
                    "`color` TEXT, " +
                    "`textColor` TEXT, " +
                    "`reminder` TEXT NOT NULL, " +
                    "`pictures` TEXT NOT NULL" +
                    ")"
            )

            database.query(
                "SELECT " +
                        "n.id, n.event_id, n.title, n.contents, " +
                        "n.date, n.color, n.remind, e.start_date " +
                        "FROM note n, event e"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    // Ignore events that are not valid in the current configuration
                    // and for which it is impossible to create valid
                    // information, such as event_id or date (which is the event date).
                    if (cursor.isNull(1) || cursor.isNull(7)) {
                        continue
                    }

                    try {
                        val reminder: Reminder = Reminder(Date(cursor.getLong(7))).apply {
                            if (cursor.getInt(6) != 0) {
                                setCustomReminder(Date(cursor.getLong(4)))
                            }
                        }


                        val values = ContentValues().apply {
                            put("id", cursor.getInt(0))
                            put("event_id", cursor.getString(1))
                            put("title", cursor.getStringOrNull(2))
                            put("contents", cursor.getStringOrNull(3) ?: "")
                            put("date", cursor.getInt(7))
                            put("color", cursor.getStringOrNull(5) ?: "#000000")
                            put("textColor", "#000000")
                            put("reminder", reminder.toJSON().toString())
                            put("pictures", JSONArray().toString())
                        }

                        database.insert("note_new", CONFLICT_NONE, values)
                    } catch (e: Exception) {}
                }
            }

            database.execSQL("DROP TABLE note")
            database.execSQL("ALTER TABLE note_new RENAME TO note")


            database.execSQL("DROP TABLE event")
            database.execSQL("CREATE TABLE IF NOT EXISTS `event` (" +
                    "`id` TEXT NOT NULL, " +
                    "`category` TEXT, " +
                    "`description` TEXT, " +
                    "`courseName` TEXT, " +
                    "`locations` TEXT NOT NULL, " +
                    "`sites` TEXT NOT NULL, " +
                    "`start` INTEGER NOT NULL, " +
                    "`end` INTEGER, " +
                    "`allday` INTEGER NOT NULL, " +
                    "`backgroundColor` TEXT, " +
                    "`textColor` TEXT, " +
                    "`note_id` INTEGER, " +
                    "PRIMARY KEY(`id`)" +
                    ")"
            )

            database.execSQL("CREATE TABLE IF NOT EXISTS `course` (" +
                    "`title` TEXT NOT NULL, " +
                    "`visible` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`title`)" +
                    ")"
            )

            database.execSQL("CREATE TABLE IF NOT EXISTS `place_info` (" +
                    "`id` TEXT, `title` TEXT NOT NULL, " +
                    "`short_desc` TEXT, `geolocalisation` TEXT NOT NULL, " +
                    "`type` TEXT NOT NULL, `photo` TEXT, " +
                    "`contact` TEXT, " +
                    "PRIMARY KEY(`title`, `type`)" +
                    ")"
            )

            database.execSQL("CREATE TABLE IF NOT EXISTS `event_change` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`type` INTEGER NOT NULL, " +
                    "`eventName` TEXT NOT NULL, " +
                    "`eventID` TEXT, " +
                    "`date` INTEGER NOT NULL, " +
                    "`dateEventChange` INTEGER NOT NULL" +
                    ")"
            )

            database.execSQL("CREATE TABLE IF NOT EXISTS `event_change` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`type` INTEGER NOT NULL, " +
                    "`eventName` TEXT NOT NULL, " +
                    "`eventID` TEXT, " +
                    "`date` INTEGER NOT NULL, " +
                    "`dateEventChange` INTEGER NOT NULL" +
                    ")"
            )
        }
    }

    // Array of all migrations
    private val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2
    )

    @Rule @JvmField
    val helper = MigrationTestHelper (
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Before
    fun instantiateOld() {
        // Create earliest version of the database.
        helper.createDatabase(TEST_DB, 1).apply {
            val event = ContentValues().apply {
                put("id", "blabla")
                put("category", "TEST")
                put("description", "Description de test")
                put("course", "Cours de test")
                put("sites", "osef")
                put("start_date", Date().time)
                put("end_date", Date().time + 1000)
                put("all_day", 0)
                put("visible", 1)
                put("background_color", "#000000")
                put("text_color", "#000000")
                put("note_id", 1)
            }

            insert("event", 0, event)

            val note = ContentValues().apply {
                put("id", 1)
                put("event_id", "blabla")
                put("title", "title")
                put("contents", "contents")
                put("color", "#000000")
                put("date", Date().time + 3000)
                put("remind", 1)
            }

            insert("note", CONFLICT_NONE, note)
            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Open latest version of the database. Room will validate the schema
        // once all migrations execute.
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            TEST_DB
        ).addMigrations(*ALL_MIGRATIONS).build().apply {
            openHelper.writableDatabase
            close()
        }
    }

}