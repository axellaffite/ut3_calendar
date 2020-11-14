package com.edt.ut3.backend.celcat

import android.content.Context
import androidx.core.text.isDigitsOnly
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.edt.ut3.R
import com.edt.ut3.backend.database.Converter
import com.edt.ut3.misc.DestructedColor
import com.edt.ut3.misc.Emoji
import com.edt.ut3.misc.extensions.fromCelcatString
import com.edt.ut3.misc.extensions.fromHTML
import com.edt.ut3.misc.extensions.getNotNull
import com.elzozor.yoda.events.EventWrapper
import kotlinx.serialization.json.*
import java.io.IOException
import java.util.*

@Entity(tableName = "event")
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
        @Throws(Exception::class)
        fun fromJSON(obj: JsonObject, classes: Set<String>, courses: Map<String, String>) = obj.run {
                val category = getNotNull("eventCategory")?.jsonPrimitive?.content?.fromHTML()
                val parsedDescription = ParsedDescription(
                    category,
                    getNotNull("description")?.jsonPrimitive?.content?.fromHTML(),
                    classes,
                    courses
                )

                val start = Date().fromCelcatString(getValue("start").jsonPrimitive.content)

                val end = getNotNull("end")?.let {
                    Date().fromCelcatString(it.jsonPrimitive.content)
                } ?: start

                val sites = getNotNull("sites")?.let { json ->
                    json.jsonArray
                        .filterIsInstance<JsonPrimitive>()
                        .map { it.content.fromHTML().trim() }
                } ?: listOf()

                Event(
                    id = getValue("id").jsonPrimitive.content.fromHTML(),
                    category = category,
                    description = parsedDescription.precisions,
                    courseName = parsedDescription.course,
                    locations = parsedDescription.classes,
                    sites = sites.sorted(),
                    start = start,
                    end = end,
                    allday = getNotNull("allDay")?.jsonPrimitive?.boolean == true
                            || getNotNull("end") == null,
                    backgroundColor = getValue("backgroundColor").jsonPrimitive.content.fromHTML(),
                    textColor = getNotNull("textColor")?.jsonPrimitive?.content?.fromHTML()
                        ?: "#000000",
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

    fun courseOrCategory(context: Context) = courseName ?: category ?: defaultName(context)

    fun defaultName(context: Context) = context.getString(R.string.default_event_name)

    /**
     * Convert the background color into a darker color.
     * In case where the background color is missing (e.g. null)
     * the primaryColor is returned by the function.
     *
     * @param context Application context
     * @return The darkened color
     */
    fun darkBackgroundColor(context: Context): Int {
        return DestructedColor.fromCelcatColor(context, backgroundColor).changeLuminosity().toArgb()
    }

    /**
     * Convert the background color into an Int and returns it.
     * In case of null background color, the primaryColor is returned.
     *
     * @param context Application context
     * @return The converted color
     */
    fun lightBackgroundColor(context: Context): Int {
        return DestructedColor.fromCelcatColor(context, backgroundColor).toArgb()
    }

    /**
     * Wrapper is used to wrap an event in order to build the views
     * and parse them with the yoda library.
     *
     * @property event The event to encapsulate.
     */
    class Wrapper(val event: Event) : EventWrapper() {
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
    class ParsedDescription(
        category: String?,
        description: String?,
        classesNames: Set<String>,
        coursesNames: Map<String, String>
    ) {
        var course: String? = null
        val classes = mutableListOf<String>()
        var teacherID: Int? = null
        var precisions: String? = null
        val regex = Regex(".*(\\[\\w+\\])")

        init {
            val precisionBuilder = StringBuilder()

            description?.lines()?.map { it.trim() }?.forEach {
                val line = regex.find(it)?.groups?.get(1)?.let { module ->
                    it.removeSuffix(module.value).trim()
                } ?: it

                when (line) {
                    in classesNames -> {
                        classes.add(line)
                    }

                    in coursesNames -> {
                        course = coursesNames[line]
                    }

                    category -> {
                        /* ignore line */
                    }

                    else -> when {
                        line.isDigitsOnly() -> {
                            teacherID = line.toInt()
                        }

                        precisionBuilder.isBlank() -> {
                            precisionBuilder.append(line)
                        }

                        else -> {
                            precisionBuilder.append("\n").append(line)
                        }
                    }
                }
            }

            precisions = if (precisionBuilder.isNotEmpty()) {
                precisionBuilder.toString()
            } else {
                null
            }

            course = cleanCourseName()
        }

        /**
         * @return The real course name without the boilerplate
         * if it has been guessed during the [event][Event] parsing,
         * or the course type.
         */
        private fun cleanCourseName(): String? {
            return try {
                course?.let {
                    val (_, newCourse) = Regex("(\\w+\\s-) (.+)").find(it)?.destructured
                        ?: throw IOException()
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