package com.edt.ut3.refactored.models.repositories.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getStringOrNull
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import arrow.core.Either
import com.edt.ut3.backend.note.Note
import com.edt.ut3.backend.note.Note.Reminder
import com.edt.ut3.refactored.models.domain.celcat.Course
import com.edt.ut3.refactored.models.domain.celcat.Event
import com.edt.ut3.refactored.models.domain.maps.Place
import com.edt.ut3.refactored.models.domain.notifications.EventChange
import com.edt.ut3.refactored.models.repositories.database.daos.*
import org.json.JSONArray
import java.util.*

@Database(entities = [Note::class, Event::class, Course::class, Place::class, EventChange::class], version = 2, exportSchema = true)
@TypeConverters(Converter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao() : EventDao
    abstract fun noteDao() : NoteDao
    abstract fun courseDao() : CourseDao
    abstract fun placeDao() : PlaceDao
    abstract fun edtChangeDao() : EdtChangeDao

    companion object {
        private var db_instance: AppDatabase? = null

        /**
         * Returns an instance of the AppDatabase.
         * The function is synchronized so it may
         * block the current thread until the
         * database is initialized.
         *
         * @param context The application context
         */
        fun getInstance(context: Context) = synchronized(this) {
            if (db_instance == null) {
                db_instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "note_event_db",
                ).addMigrations(MIGRATION_1_2).build()
            }

            db_instance!!
        }


        val MIGRATION_1_2 = object:  Migration(1,2) {
            override fun migrate(database: SupportSQLiteDatabase) {

                // Here we create a new table to insert all the
                // row with the proper constraints.
                // It will be renamed into 'note' at the end of
                // the transaction.
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

                // We then select all the columns from the old
                // table to fill the new one.
                database.query("SELECT " +
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
                            // Extract the event date from the 7th
                            // column of the cursor.
                            val reminder: Reminder = Reminder(Date(cursor.getLong(7))).apply {
                                // If the remind column (6th) is set to true
                                // edit the current reminder to set it to
                                // a custom reminder with the proper date (4th).
                                if (cursor.getInt(6) != 0) {
                                    setCustomReminder(Date(cursor.getLong(4)))
                                }
                            }

                            // Here we create the row with proper values
                            // and we set a default value for each row
                            // were there is no value specified.
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

                            // Finally we insert the row into the database.
                            database.insert("note_new", SQLiteDatabase.CONFLICT_NONE, values)
                        } catch (e: Exception) {}
                    }
                }

                // Final requests for the note table,
                // we drop the old one (must be done before the event one
                // in order to preserve the foreign key constraint)
                // and we rename the new one (note_new) into note.
                database.execSQL("DROP TABLE note")
                database.execSQL("ALTER TABLE note_new RENAME TO note")



                // Here we drop the old event table
                // and create the new one.
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

                // All the remaining table have no special
                // constraint and were not present in the
                // previous version, we don't need to
                // ensure their compatibilities, just create them.
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
    }


}