package com.edt.ut3.refactored.models.services.celcat

import com.edt.ut3.misc.extensions.updateFromCelcatDate
import com.edt.ut3.misc.extensions.fromHTML
import com.edt.ut3.misc.extensions.getNotNull
import com.edt.ut3.refactored.models.domain.celcat.Event
import com.edt.ut3.refactored.models.domain.celcat.ParsedDescription
import com.edt.ut3.refactored.models.domain.celcat.fromCelcatDescription
import kotlinx.serialization.json.*
import java.util.*

class CelcatConverter {
    @Throws(Exception::class)
    fun fromJSON(obj: JsonObject, classes: Set<String>, courses: Map<String, String>) = obj.run {
        val category = getCategory()
        val parsedDescription = parseDescription(category, classes, courses)
        val start = getStartDate()
        val end = getEndDate(start)
        val sites = getSites()

        Event(
            id = stringFromHtml("id"),
            category = category,
            description = parsedDescription.precisions,
            courseName = parsedDescription.course,
            locations = parsedDescription.classes,
            sites = sites.sorted(),
            start = start,
            end = end,
            allday = isAllDay(),
            backgroundColor = getBackgroundColor(),
            textColor = getTextColor(),
            noteID = null
        )
    }


    private fun JsonObject.getCategory() = nullableStringFromHtml("eventCategory")

    private fun JsonObject.parseDescription(
        category: String?,
        classes: Set<String>,
        courses: Map<String, String>
    ) = ParsedDescription.fromCelcatDescription(
        category = category,
        description = nullableStringFromHtml("description"),
        classesNames = classes,
        coursesNames = courses
    )

    private fun JsonObject.getStartDate() =
        Date().updateFromCelcatDate(stringFromHtml("start"))

    private fun JsonObject.getEndDate(start: Date) =
        nullableStringFromHtml("end")?.let(Date()::updateFromCelcatDate) ?: start

    private fun JsonObject.getSites() = getNotNull("sites")?.let { json ->
        json.jsonArray
            .filterIsInstance<JsonPrimitive>()
            .map { it.content.fromHTML().trim() }
    } ?: emptyList()

    private fun JsonObject.isAllDay() = (
        getNotNull("allDay")?.jsonPrimitive?.boolean == true ||
            getNotNull("end") == null
        )

    private fun JsonObject.getBackgroundColor() = stringFromHtml("backgroundColor")

    private fun JsonObject.getTextColor() = nullableStringFromHtml("textColor") ?: "#000000"

    private fun JsonObject.stringFromHtml(key: String) = requireNotNull(nullableStringFromHtml(key))

    private fun JsonObject.nullableStringFromHtml(key: String): String? {
        return getNotNull(key)?.jsonPrimitive?.contentOrNull?.fromHTML()
    }
}