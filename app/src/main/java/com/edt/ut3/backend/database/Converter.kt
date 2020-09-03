package com.edt.ut3.backend.database

import androidx.room.TypeConverter
import com.edt.ut3.backend.note.Picture
import com.edt.ut3.misc.map
import com.edt.ut3.misc.toList
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
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

    @TypeConverter
    fun serializePicture(pictures: List<Picture>) = JSONArray(
        pictures.map {
            JSONObject().apply {
                put("picture", it.picture)
                put("thumbnail", it.thumbnail)
            }
        }
    ).toString()

    @TypeConverter
    fun deserializePicture(str: String) = JSONArray(str).map {
        (it as JSONObject).run {
            Picture(getString("picture"), getString("thumbnail"))
        }
    }
}