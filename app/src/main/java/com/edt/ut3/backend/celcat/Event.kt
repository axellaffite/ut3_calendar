package com.edt.ut3.backend.celcat

import androidx.room.*
import com.edt.ut3.backend.database.Converter
import com.edt.ut3.backend.note.Note
import com.edt.ut3.misc.Emoji
import com.edt.ut3.misc.fromCelcatString
import com.edt.ut3.misc.fromHTML
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
        fun fromJSON(obj: JSONObject, classes: Set<String>, courses: Set<String>) = obj.run {
            val parsedDescription = ParsedDescription(optString("description")?.fromHTML(), classes, courses)
            val category = optString("eventCategory")?.fromHTML()
            val start = Date().apply { fromCelcatString(getString("start")) }
            val end = if (isNull("end")) start else Date().apply { fromCelcatString(getString("end")) }

            Event(
                id = getString("id").fromHTML(),
                category = category,
                description = parsedDescription.precisions,
                courseName = parsedDescription.course,
                sites = optJSONArray("sites")?.toList<String?>()?.filterNotNull()?.map { it.fromHTML().trim() } ?: listOf(),
                start = start,
                end = end,
                allday = getBoolean("allDay"),
                backGroundColor = getString("backgroundColor").fromHTML(),
                textColor = optString("textColor")?.fromHTML() ?: "#000000",
                noteID = null
            )
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

    /**
     * Wrapper is used to wrap an event in order to build the views
     * and parse them with the yoda library.
     *
     * @property event The event to encapsulate.
     */
    class Wrapper(val event: Event): EventWrapper() {
        override fun begin() = event.start

        override fun end() = event.end ?: event.start
    }


    /**
     * ParsedDescription is used to parse the description
     * and dispatch its contents into different variables.
     *
     *
     * @param description The description to parse
     * @param classesNames All the classes names that exists (otherwise classes will always be empty)
     * @param coursesNames All the courses names that exists (otherwise course will always be null)
     */
    class ParsedDescription(description: String?, classesNames: Set<String>, coursesNames: Set<String>) {
        var course: String? = null
        val classes = mutableListOf<String>()
        var teacherID: Int? = null
        var precisions: String? = null

        init {
            description?.trim()?.lines()?.forEach {
                val precisionBuilder = StringBuilder()

                when {
                    it.matches(Regex("\\d\\*")) -> { teacherID = it.toInt() }
                    classesNames.contains(it) -> classes.add(it)
                    coursesNames.contains(it) -> course = it
                    else -> precisionBuilder.append(it)
                }

                precisions = if (precisionBuilder.isNotEmpty()) {
                    precisionBuilder.toString()
                } else { null }
            }
        }
    }
}