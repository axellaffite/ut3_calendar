package com.elzozor.ut3calendar.backend.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.elzozor.ut3calendar.backend.celcat.Event
import com.elzozor.ut3calendar.backend.database.daos.EventDao
import com.elzozor.ut3calendar.backend.database.daos.NoteDao
import com.elzozor.ut3calendar.backend.note.Note

@Database(entities = [Note::class, Event::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao() : EventDao
    abstract fun noteDao() : NoteDao

    companion object {
        private var db_instance: AppDatabase? = null

        @Synchronized
        fun getInstance(context: Context) : AppDatabase {
            if (db_instance == null) {
                db_instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "note_event_db"
                ).build()
            }

            return db_instance!!
        }
    }
}