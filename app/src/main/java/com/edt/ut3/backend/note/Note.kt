package com.edt.ut3.backend.note

import androidx.room.*
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.Converter
import java.io.File
import java.util.*

@Entity(tableName = "note",
    foreignKeys = [
        ForeignKey(entity = Event::class,
            parentColumns = ["id"],
            childColumns = ["event_id"]
        )
    ], indices = [Index("event_id")])
data class Note(
    @PrimaryKey(autoGenerate = true) var id: Long,
    @ColumnInfo(name = "event_id") var eventID: String?,
    var title: String?,
    var contents: String,
    @TypeConverters(Converter::class) var date: Date,
    var color: String?,
    var textColor: String?,
    var reminder: Boolean = false,
    @TypeConverters(Converter::class) val pictures: MutableList<Picture> = mutableListOf())
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
        fun generateEmptyNote(title: String? = null, eventID: String? = null) = Note(0L, eventID, title, "", Date(), null, null, false)
    }

    fun clearPictures() {
        while (pictures.isNotEmpty()) {
            removePictureAt(pictures.lastIndex)
        }
    }

    fun removePictureAt(position: Int) {
        if (position in 0 .. pictures.lastIndex) {
            val picture = pictures[position]
            pictures.removeAt(position)
            cleanPictureData(picture)
        }
    }

    private fun removePicture(picture: Picture) {
        pictures.remove(picture)
        cleanPictureData(picture)
    }

    private fun cleanPictureData(picture: Picture) {
        File(picture.picture).delete()
        File(picture.thumbnail).delete()
    }

}