package com.edt.ut3.backend.celcat

import androidx.room.*
import com.edt.ut3.backend.database.Converter
import com.edt.ut3.backend.note.Note
import com.edt.ut3.misc.Emoji
import com.edt.ut3.misc.fromCelcatString
import com.edt.ut3.misc.toList
import com.elzozor.yoda.events.EventWrapper
import org.json.JSONException
import org.json.JSONObject
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
    @TypeConverters(Converter::class) var sites: List<String>,
    @TypeConverters(Converter::class) var start: Date,
    @TypeConverters(Converter::class) var end: Date?,
    var allday: Boolean,
    var backGroundColor: String?,
    var textColor: String?,
    @ColumnInfo(name = "note_id") var noteID: Long?
) {
    companion object {
        @Throws(JSONException::class)
        fun fromJSON(obj: JSONObject) = obj.run {
            val category = optString("eventCategory")
            val course = parseCourse(optString("description")) ?: category
            val start = Date().apply { fromCelcatString(getString("start")) }
            val end = if (isNull("end")) start else Date().apply { fromCelcatString(getString("end")) }

            Event(
                id = getString("id"),
                category = category,
                description = parseDescription("description"),
                courseName = course,
                sites = optJSONArray("sites")?.toList<String?>()?.filterNotNull() ?: listOf(),
                start = start,
                end = end,
                allday = getBoolean("allDay"),
                backGroundColor = getString("backgroundColor"),
                textColor = optString("textColor") ?: "#000000",
                noteID = null
            )
        }

        private fun parseDescription(description: String?) : String? =
            description?.run {
                if (isNullOrBlank()) return null

                this.trim().lines().let { lines ->
                    if (lines.size == 1) {
                        return description
                    }

                    return lines.subList(1, lines.size).joinToString(separator = "\n")
                }
            }

        private fun parseCourse(description: String?): String? =
            description?.run {
                if (isNullOrBlank()) return null

                this.trim().lines().let { lines ->
                    lines.forEach {
                        if (it.matches(Regex("^\\w*\\s+-\\s+.*$"))) {
                            return it
                        }
                    }

                    if (lines.size > 1) {
                        return lines[1]
                    }

                    return null
                }
            }

    }

    fun categoryWithEmotions(): String? {
        return category?.let {
            when {
                it.contains("controle", true) ||
                    it.contains("examen", true) -> "$category ${Emoji.sad()}"

                else -> category
            }
        }
    }

    class Wrapper(val event: Event): EventWrapper() {
        override fun begin() = event.start

        override fun end() = event.end ?: event.start
    }
}