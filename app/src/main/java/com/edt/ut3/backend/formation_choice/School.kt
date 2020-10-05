package com.edt.ut3.backend.formation_choice

import com.edt.ut3.misc.map
import org.json.JSONObject

data class School(
    val name: String,
    val info: List<Info>
){
    companion object {
        fun fromJSON(json: JSONObject) = School (
            name = json.getString("name"),
            info = json.getJSONArray("infos").map {
                Info.fromJSON(it as JSONObject)
            }
        )
    }

    data class Info (
        val url: String,
        val groups: String,
        val rooms: String,
        val courses: String
    ){
        companion object {
            fun fromJSON(json: JSONObject) = Info (
                url = json.getString("url"),
                groups = json.getString("groups"),
                rooms = json.getString("rooms"),
                courses = json.getString("courses")
            )
        }
    }
}