package com.edt.ut3.refactored.models.domain.notifications

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.edt.ut3.refactored.models.repositories.database.Converter
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
}
