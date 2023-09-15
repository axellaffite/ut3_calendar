package com.edt.ut3.backend.database

import androidx.room.TypeConverter
import com.edt.ut3.backend.note.Note
import com.edt.ut3.backend.note.Picture
import com.edt.ut3.misc.extensions.map
import com.edt.ut3.misc.extensions.toList
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
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
    fun serializePictureList(pictures: MutableList<Picture>) = JSONArray(
        pictures.map {
            JSONObject().apply {
                put("picture", it.picture)
                put("thumbnail", it.thumbnail)
            }
        }
    ).toString()

    @TypeConverter
    fun deserializePictureList(str: String): MutableList<Picture> = JSONArray(str).map {
        (it as JSONObject).run {
            Picture(picture = getString("picture"), thumbnail = getString("thumbnail"))
        }
    }.toMutableList()

    @TypeConverter
    fun serializeGeoPoint(geoPoint: GeoPoint) = JSONObject().apply {
        put("lat", geoPoint.latitude)
        put("lon", geoPoint.longitude)
    }.toString()

    @TypeConverter
    fun deserializeGeoPoint(str: String) = GeoPoint(0.0,0.0).apply {
        JSONObject(str).run {
            latitude = getDouble("lat")
            longitude = getDouble("lon")
        }.toString()
    }

    @TypeConverter
    fun serializeReminder(reminder: Note.Reminder) = reminder.toJSON().toString()

    @TypeConverter
    fun deserializeReminder(str: String) = Note.Reminder.fromJSON(str)
}