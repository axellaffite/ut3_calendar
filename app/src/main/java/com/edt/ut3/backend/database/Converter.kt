package com.edt.ut3.backend.database

import androidx.room.TypeConverter
import com.edt.ut3.misc.toList
import org.json.JSONArray
import org.json.JSONException
import java.util.*

class Converter {
    @Throws(JSONException::class)
    @TypeConverter
    fun toList(string: String) = JSONArray(string).toList<String>()

    @TypeConverter
    fun toString(list: List<String>): String {
        return JSONArray(list).toString()
    }

    @TypeConverter
    fun toTimestamp(date: Date) = date.time

    @TypeConverter
    fun fromTimestamp(time: Long) = Date(time)
}