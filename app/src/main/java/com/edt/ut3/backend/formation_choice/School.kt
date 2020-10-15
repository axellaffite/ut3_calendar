package com.edt.ut3.backend.formation_choice

import android.net.Uri
import androidx.core.net.toUri
import com.edt.ut3.R
import com.edt.ut3.misc.extensions.map
import com.edt.ut3.misc.extensions.toJSONArray
import org.json.JSONException
import org.json.JSONObject

data class School(
    val name: String,
    val info: List<Info>
){
    companion object {
        @Throws(JSONException::class)
        fun fromJSON(json: JSONObject) = School (
            name = json.getString("name"),
            info = json.getJSONArray("infos").map {
                Info.fromJSON(it as JSONObject)
            }
        )

        val default = School(
            name="Université Paul Sabatier",
            info = listOf(
                Info(
                    name="FSI",
                    url="https://edt.univ-tlse3.fr/calendar2",
                    groups="https://edt.univ-tlse3.fr/calendar2/Home/ReadResourceListItems?myResources=false&searchTerm=___&pageSize=10000&pageNumber=1&resType=103&_=1601408259547",
                    rooms="https://edt.univ-tlse3.fr/calendar2/Home/ReadResourceListItems?myResources=false&searchTerm=___&pageSize=10000&pageNumber=1&resType=102&_=1601408259546",
                    courses="https://edt.univ-tlse3.fr/calendar2/Home/ReadResourceListItems?myResources=false&searchTerm=___&pageSize=10000&pageNumber=1&resType=100&_=1601408259545"
                )
            )
        )
    }

    fun toJSON() = JSONObject().apply {
        put("name", name)
        put("infos", info.toJSONArray { it.toJSON() })
    }

    data class Info (
        val name: String,
        val url: String,
        val groups: String,
        val rooms: String,
        val courses: String
    ){
        companion object {
            @Throws(JSONException::class)
            fun fromJSON(json: JSONObject) = Info (
                name = json.getString("name"),
                url = json.getString("url"),
                groups = json.getString("groups"),
                rooms = json.getString("rooms"),
                courses = json.getString("courses")
            )

            private fun extractFids(uri: Uri): List<String> {
                fun extract(index: Int): List<String> {
                    val fid = uri.getQueryParameter("fid$index")
                    if (fid.isNullOrBlank()) {
                        return listOf()
                    }

                    return extract(index + 1) + fid
                }

                return extract(0)
            }


            @Throws(InvalidLinkException::class)
            fun fromClassicLink(link: String): Pair<Info, List<String>> {
                try {
                    try {
                        val baseLinkFinder = Regex("(.*)/cal.*")
                        val baseLink = baseLinkFinder.find(link)?.value
                        val fids = extractFids(link.toUri())

                        if (baseLink.isNullOrBlank()) {
                            throw InvalidLinkException(R.string.error_invalid_link)
                        }

                        if (fids.isEmpty()) {
                            throw InvalidLinkException(R.string.error_link_groups)
                        }

                        val name = ""
                        println(baseLink)
                        val url = baseLinkFinder.find(link)?.groups?.get(1)?.value!!
                        val groups = guessGroupsLink(url)
                        val rooms = guessRoomsLink(url)
                        val courses = guessCoursesLink(url)

                        return Pair(Info(name, url, groups, rooms, courses), fids)
                    } catch (e: UnsupportedOperationException) {
                        throw InvalidLinkException(R.string.error_invalid_link)
                    }
                } catch (e: InvalidLinkException) {
                    throw e
                }
            }

            private fun guessGroupsLink(link: String): String {
                val search =
                    if (link.contains("calendar2")) { "___" }
                    else { "__" }

                return "$link/Home/ReadResourceListItems?myResources=false&searchTerm=$search&pageSize=1000000&pageNumber=1&resType=103"
            }

            private fun guessRoomsLink(link: String): String {
                val search =
                    if (link.contains("calendar2")) { "___" }
                    else { "__" }

                return "$link/Home/ReadResourceListItems?myResources=false&searchTerm=$search&pageSize=1000000&pageNumber=1&resType=102"
            }

            private fun guessCoursesLink(link: String): String {
                val search =
                    if (link.contains("calendar2")) { "___" }
                    else { "__" }

                return "$link/Home/ReadResourceListItems?myResources=false&searchTerm=$search&pageSize=10000000&pageNumber=1&resType=100"
            }
        }

        fun toJSON() = JSONObject().apply {
            put("name", name)
            put("url", url)
            put("groups", groups)
            put("rooms", rooms)
            put("courses", courses)
        }

        class InvalidLinkException(val reason: Int): Exception()

        data class Group (
            val id: String,
            val text: String
        ) {
            companion object {
                @Throws(JSONException::class)
                fun fromJSON(json: JSONObject) = Group (
                    id = json.getString("id"),
                    text = json.getString("text")
                )
            }

            fun toJSON() = JSONObject().apply {
                put("id", id)
                put("text", text)
            }
        }
    }
}