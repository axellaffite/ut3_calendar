package com.elzozor.ut3calendar.backend.note

import androidx.room.*
import com.elzozor.ut3calendar.backend.celcat.Event
import java.util.*

@Entity(tableName = "note",
    foreignKeys = [
        ForeignKey(entity = Event::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        )
    ], indices = [Index("event_id")])
class Note(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "event_id") var eventID: String?,
    var title: String?,
    var contents: String,
    var date: Date,
    var color: String?,
    var reminder: Boolean = false
)