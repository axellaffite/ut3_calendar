package com.edt.ut3.backend.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.edt.ut3.backend.celcat.Course
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.daos.*
import com.edt.ut3.backend.maps.Place
import com.edt.ut3.backend.note.Note
import com.edt.ut3.backend.notification.EventChange

@Database(entities = [Note::class, Event::class, Course::class, Place::class, EventChange::class], version = 2, exportSchema = false)
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
                ).fallbackToDestructiveMigration().build()
            }

            db_instance!!
        }
    }
}