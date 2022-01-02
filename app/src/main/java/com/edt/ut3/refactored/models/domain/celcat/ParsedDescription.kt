package com.edt.ut3.refactored.models.domain.celcat

import androidx.core.text.isDigitsOnly

/**
 * ParsedDescription is used to parse the description
 * and dispatch its contents into different variables.
 */
data class ParsedDescription(
    val classes: List<String>,
    val course: String? = null,
    val teacherID: Int? = null,
    val precisions: String? = null
) {
    companion object
}


/**
 * @param description The description to parse
 * @param classesNames All the classes names that exists (otherwise classes will always be empty)
 * @param coursesNames All the courses names that exists (otherwise course will always be null)
 */
fun ParsedDescription.Companion.fromCelcatDescription(
    category: String?,
    description: String?,
    classesNames: Set<String>,
    coursesNames: Map<String, String>
): ParsedDescription {
    val regex = Regex(".*(\\[\\w+\\])")

    val classes = mutableListOf<String>()
    var course: String? = null
    var teacherID: Int? = null
    val precisions: String?

    val precisionBuilder = StringBuilder()

    description?.lines()?.map { it.trim() }?.forEach {
        val line = regex.find(it)?.groups?.get(1)?.let { module ->
            it.removeSuffix(module.value).trim()
        } ?: it

        when (line) {
            in classesNames -> classes.add(line)
            in coursesNames -> course = coursesNames[line]
            category -> { /* ignore line */
            }

            else -> when {
                line.isDigitsOnly() -> teacherID = line.toInt()
                precisionBuilder.isBlank() -> precisionBuilder.append(line)
                else -> precisionBuilder.append("\n").append(line)
            }
        }
    }

    precisions = precisionBuilder.takeIf { it.isNotBlank() }?.toString()
    course = cleanCourseName(course)

    return ParsedDescription(classes, course, teacherID, precisions)
}

/**
 * @return The real course name without the boilerplate
 * if it has been guessed during the [event][Event] parsing,
 * or the course type.
 */
private fun cleanCourseName(course: String?): String? = course?.let {
    val regex = Regex("(\\w+\\s-) (.+)")
    val match = regex.find(course)
    return match?.let { it.groups[2]?.value }
}