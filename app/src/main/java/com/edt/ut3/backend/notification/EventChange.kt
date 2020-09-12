package com.edt.ut3.backend.notification

import android.content.Context
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.edt.ut3.R
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.database.Converter
import java.util.*

data class EventChange(
    @PrimaryKey(autoGenerate = true)
    val id : Long,
    val type : Int,
    val eventName : String,
    val eventID : String?,
    @TypeConverters(Converter::class)
    val date : Date,
    @TypeConverters(Converter::class)
    val dateEventChange : Date
)
{
    companion object
    {
        public suspend fun newEventChangeNewCourse(context : Context,event : Event)
        {
            val courseName  = if (event.courseName.isNullOrEmpty()) {context.getString(R.string.cours) } else { event.courseName!!}
            val newEventChange = EventChange(0L,0,courseName,event.id,event.start,Date())
            AppDatabase.getInstance(context).edtChangeDao().insert(newEventChange)
        }
        public suspend fun newEventChangeRemoveCourse(context : Context,event : Event)
        {
            val courseName  = if (event.courseName.isNullOrEmpty()) {context.getString(R.string.cours) } else { event.courseName!!}
            val newEventChange = EventChange(0L,1,courseName,event.id,event.start,Date())
            AppDatabase.getInstance(context).edtChangeDao().insert(newEventChange)
        }
        public suspend fun newEventChangeUpdated(context : Context,event : Event)
        {
            val courseName  = if (event.courseName.isNullOrEmpty()) {context.getString(R.string.cours) } else { event.courseName!!}
            val newEventChange = EventChange(0L,2,courseName,event.id,event.start,Date())
            AppDatabase.getInstance(context).edtChangeDao().insert(newEventChange)
        }
    }

}
