package com.edt.ut3.backend.notification

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.Converter
import com.edt.ut3.backend.database.viewmodels.EdtChangeViewModel
import java.util.*

@Entity(tableName = "event_change")
data class EventChange(
    @PrimaryKey(autoGenerate = true) var id : Long,
    val type : Int,
    val eventName : String,
    val eventID : String?,
    @TypeConverters(Converter::class) val date : Date,
    @TypeConverters(Converter::class) val dateEventChange : Date)
{
    enum class Type(val value: Int) { ADDED(0), REMOVED(1), UPDATED(2) }


    companion object {
        suspend fun newEventChangeNewCourse(context : Context, event : Event) =
            buildEventChange(context, event, Type.ADDED)

        suspend fun newEventChangeRemoveCourse(context : Context, event : Event) =
            buildEventChange(context, event, Type.REMOVED)

        suspend fun newEventChangeUpdated(context : Context, event : Event) =
            buildEventChange(context, event, Type.UPDATED)


        private suspend fun buildEventChange(context : Context, event : Event, type: Type) {
            val courseName =
                if (! event.courseName.isNullOrEmpty()) { event.courseName!! }
                else { context.getString(R.string.cours) }

            val newEventChange = EventChange(0L, type.value, courseName, event.id, event.start, Date())
            EdtChangeViewModel(context).insert(newEventChange)
        }
    }

}
