package com.edt.ut3.backend.requests.celcat

import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.misc.extensions.fromHTML
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize

data class CoursesRequest(
    @field:JsonDeserialize(using = CourseDeserializer::class)
    val results: Map<String, String>
)

class CourseDeserializer : JsonDeserializer<Map<String, String>>() {
    data class JsonCourse(val id: String, val text: String)

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Map<String, String> {
        val node: JsonNode = p.codec.readTree(p)
        val courses = mutableMapOf<String, String>()

        node.forEach { courseNode ->
            val id = courseNode.get("id").asText()
            val text = courseNode.get("text").asText()
            courses[id] = text
            courses[text] = text
        }

        return courses
    }
}


data class ClassesRequest(
    @field:JsonDeserialize(using = ClassDeserializer::class)
    val results: List<String>
)

class ClassDeserializer : JsonDeserializer<List<String>>() {
    data class JsonClass(val id: String)

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): List<String> {
        val node: JsonNode = p.codec.readTree(p)
        return node.map { it.get("id").asText().fromHTML().trim() }
    }
}


data class SchoolsRequest(val entries: List<School>)


data class GroupsRequest(val results: List<School.Info.Group>)