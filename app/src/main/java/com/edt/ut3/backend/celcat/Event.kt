package com.edt.ut3.backend.celcat

import androidx.room.*
import com.edt.ut3.backend.note.Note
import com.edt.ut3.misc.Emoji
import org.json.JSONArray
import org.json.JSONException
import java.util.*

@Entity(tableName = "event",
    foreignKeys = [
        ForeignKey(entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["note_id"]
        )
    ], indices = [Index("note_id")]
)
data class Event(
    @PrimaryKey var id: String,
    var category: String?,
    var description: String?,
    var courseName: String?,
    var sites: List<String>,
    var start: Date,
    var end: Date?,
    var allday: Boolean,
    var backGroundColor: String?,
    var textColor: String?,
    var noteID: Long?
) {
    fun categoryWithEmotions(): String? {
        return category?.let {
            when {
                it.contains("controle", true) ||
                    it.contains("examen", true) -> "$category ${Emoji.sad()}"

                else -> category
            }
        }
    }

    class Converter {
        @Throws(JSONException::class)
        @TypeConverter
        fun toList(string: String): List<String> {
            val jsonArr = JSONArray(string)
            val arr = mutableListOf<String>()
            for (i in 0 until jsonArr.length()) {
                arr.add(jsonArr.getString(i))
            }

            return arr
        }

        @TypeConverter
        fun toString(list: List<String>): String {
            return JSONArray(list).toString()
        }

        @TypeConverter
        fun toTimestamp(date: Date) = date.time

        @TypeConverter
        fun fromTimestamp(time: Long) = Date(time)
    }
}