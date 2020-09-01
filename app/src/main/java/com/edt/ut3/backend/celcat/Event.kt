package com.edt.ut3.backend.celcat

import android.content.Context
import androidx.room.*
import com.edt.ut3.backend.database.Converter
import com.edt.ut3.backend.note.Note
import com.edt.ut3.misc.*
import com.elzozor.yoda.events.EventWrapper
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
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
    @TypeConverters(Converter::class) var locations: List<String>,
    @TypeConverters(Converter::class) var sites: List<String>,
    @TypeConverters(Converter::class) var start: Date,
    @TypeConverters(Converter::class) var end: Date?,
    var allday: Boolean,
    var backgroundColor: String?,
    var textColor: String?,
    @ColumnInfo(name = "note_id") var noteID: Long?
) {
    companion object {
        @Throws(JSONException::class)
        fun fromJSON(obj: JSONObject, classes: Set<String>, courses: Set<String>) = obj.run {
            val parsedDescription = ParsedDescription(optString("description").fromHTML(), classes, courses)

            val category = optString("eventCategory").fromHTML()
            val start = Date().apply { fromCelcatString(getString("start")) }
            val end = if (isNull("end")) start else Date().apply { fromCelcatString(getString("end")) }

            Event(
                id = getString("id").fromHTML(),
                category = category,
                description = parsedDescription.precisions,
                courseName = parsedDescription.course,
                locations = parsedDescription.classes,
                sites = optJSONArray("sites")?.toList<String?>()?.filterNotNull()?.map { it.fromHTML().trim() } ?: listOf(),
                start = start,
                end = end,
                allday = getBoolean("allDay") || isNull("end"),
                backgroundColor = getString("backgroundColor").fromHTML(),
                textColor = optString("textColor").fromHTML() ?: "#000000",
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
     * Convert the background color into a darker color.
     * In case where the background color is missing (e.g. null)
     * the primaryColor is returned by the function.
     *
     * @param context Application context
     * @return The darkened color
     */
    fun darkBackgroundColor(context: Context) : Int {
        return DestructedColor.fromCelcatColor(context, backgroundColor).changeLuminosity().toArgb()
    }

    /**
     * Convert the background color into an Int and returns it.
     * In case of null background color, the primaryColor is returned.
     *
     * @param context Application context
     * @return The converted color
     */
    fun lightBackgroundColor(context: Context) : Int {
        return DestructedColor.fromCelcatColor(context, backgroundColor).toArgb()
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

        override fun isAllDay() = event.allday
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
            val precisionBuilder = StringBuilder()

            description?.lines()?.map { it.trim() }?.forEach {
                println("Line: $it")
                when {
                    it.matches(Regex("\\d\\*")) -> { teacherID = it.toInt() }
                    classesNames.contains(it) -> classes.add(it)
                    coursesNames.contains(it) -> course = it
                    else -> if (precisionBuilder.isBlank()) {
                        precisionBuilder.append(it)
                    } else {
                        precisionBuilder.append("\n").append(it)
                    }
                }
            }

            precisions = if (precisionBuilder.isNotEmpty()) {
                precisionBuilder.toString()
            } else { null }

            course = cleanCourseName()
        }

        private fun cleanCourseName(): String? {
            return try {
                course?.let {
                    val (_, newCourse) = Regex("(\\w+\\s-) (.+)").find(it)?.destructured ?: throw IOException()
                    newCourse
                }
            } catch (e: IOException) {
                course
            }
        }

        override fun toString() = """course:${course}
            |classes:${classes}
            |teacherID:${teacherID}
            |precisions:${precisions}
            |
            |
        """.trimMargin()
    }
}