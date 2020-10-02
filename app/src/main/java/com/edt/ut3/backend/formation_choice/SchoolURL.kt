package com.edt.ut3.backend.formation_choice

import com.edt.ut3.misc.map
import org.json.JSONObject

data class SchoolURL(
    val name: String,
    val urls: List<String>,
    val groups: String,
    val rooms: String,
    val courses: String
){
    companion object {
        fun fromJSON(json: JSONObject) = SchoolURL (
            name = json.getString("name"),
            urls = json.getJSONArray("urls").map { it as String },
            groups = json.getString("groups"),
            rooms = json.getString("rooms"),
            courses = json.getString("courses")
        )
    }
}