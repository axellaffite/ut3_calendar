package com.edt.ut3.backend.note

import android.content.Context
import androidx.room.*
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.database.Converter
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.util.*

@Entity(tableName = "note",
    foreignKeys = [
        ForeignKey(entity = Event::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        )
    ], indices = [Index("event_id")])
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "event_id") var eventID: String?,
    var title: String?,
    var contents: String,
    @TypeConverters(Converter::class) var date: Date,
    var color: String?,
    var textColor: String?,
    var reminder: Boolean = false,
    @TypeConverters(Converter::class) var pictures: List<Picture> = listOf())
{

    private constructor(id: Long, note: Note): this(
        id = id,
        eventID = note.eventID,
        title = note.title,
        contents = note.contents,
        date = note.date,
        color = note.color,
        textColor = note.textColor,
        reminder = note.reminder,
        pictures = note.pictures
    )

    companion object {
        fun generateEmptyNote(eventID: String? = null) = Note(0L, eventID, null, "", Date(), null, null, false)

        suspend fun saveNote(note: Note, context: Context): Note = withContext(IO) {
            AppDatabase.getInstance(context).noteDao().let {
                val ids = it.insert(note)

                if (note.id == 0L) {
                    Note(ids[0], note)
                } else {
                    note
                }
            }
        }
    }

}