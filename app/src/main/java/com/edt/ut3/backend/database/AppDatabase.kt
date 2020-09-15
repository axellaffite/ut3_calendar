package com.edt.ut3.backend.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.edt.ut3.backend.celcat.Course
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.daos.CourseDao
import com.edt.ut3.backend.database.daos.EventDao
import com.edt.ut3.backend.database.daos.NoteDao
import com.edt.ut3.backend.database.daos.PlaceDao
import com.edt.ut3.backend.maps.Place
import com.edt.ut3.backend.note.Note

@Database(entities = [Note::class, Event::class, Course::class, Place::class], version = 2, exportSchema = false)
@TypeConverters(Converter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao() : EventDao
    abstract fun noteDao() : NoteDao
    abstract fun courseDao() : CourseDao
    abstract fun placeDao() : PlaceDao

    companion object {
        private var db_instance: AppDatabase? = null

        @Synchronized
        fun getInstance(context: Context) : AppDatabase {
            if (db_instance == null) {
                db_instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "note_event_db",
                ).fallbackToDestructiveMigration().build()
            }

            return db_instance!!
        }
    }
}