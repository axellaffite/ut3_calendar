package com.edt.ut3.backend.formation_choice

import android.net.Uri
import androidx.core.net.toUri
import com.edt.ut3.R
import com.edt.ut3.backend.background_services.updaters.ResourceType
import com.edt.ut3.backend.formation_choice.School.Info
import kotlinx.serialization.Serializable

/**
 * Represents a School by its name and [information][Info].
 *
 * @property name
 * @property info
 */
@Serializable
data class School(
    val name: String,
    val info: List<Info>
){
    companion object {
        /**
         * Returns the default School which is Paul Sabatier
         * with all its [information][Info].
         */
        val default = School(
            name="IUT MFJA",
            info = listOf(
                Info(
                    name="MJFA",
                    url="https://edt.iut-tlse3.fr/calendar-mfja",
                    groups="https://edt.iut-tlse3.fr/calendar-mfja/Home/ReadResourceListItems?myResources=false&searchTerm=__&pageSize=100000&pageNumber=1&resType=103",
                    rooms="https://edt.iut-tlse3.fr/calendar-mfja/Home/ReadResourceListItems?myResources=false&searchTerm=__&pageSize=100000&pageNumber=1&resType=102",
                    courses="https://edt.iut-tlse3.fr/calendar-mfja/Home/ReadResourceListItems?myResources=false&searchTerm=        __&pageSize=100000&pageNumber=1&resType=100"
                )
            )
        )
    }

    /**
     * Represent a [school][School] information.
     *
     * @property name The faculty name
     * @property url The faculty schedule url
     * @property groups The link to get all the groups
     * @property rooms The link to get all the rooms
     * @property courses The link to get all the courses
     */
    @Serializable
    data class Info (
        val name: String,
        val url: String,
        val groups: String,
        val rooms: String,
        val courses: String
    ){
        fun get(resourceType: ResourceType?) = when (resourceType) {
            ResourceType.Groups -> groups
            ResourceType.Courses -> courses
            null -> groups
        }


        companion object {
            /**
             * This function extracts the fids from a Celcat URL.
             *
             * @param uri The celcat url
             * @return All found fids.
             */
            private fun extractFids(uri: Uri): List<String> {
                /**
                 * Extract the fids until the next fid
                 * is null or blank.
                 *
                 * @param index The current index (default: 0)
                 * @return The fid list
                 */
                fun extract(index: Int = 0): List<String> {
                    val fid = uri.getQueryParameter("fid$index")
                    if (fid.isNullOrBlank()) {
                        return listOf()
                    }

                    return extract(index + 1) + fid
                }

                return extract()
            }



            val celcatLinkPattern = Regex("(.*)/cal.*")
            /**
             * Try to converts a classic link to and [Info].
             * The link must match [this pattern][celcatLinkPattern]
             *
             * @InvalidLinkException If the link isn't valid. It contains the error
             * as an ID which is traduced in several languages.
             * @param link The link to parse
             * @return A pair containing an [Info] and a list of fids.
             */
            @Throws(InvalidLinkException::class)
            fun fromClassicLink(link: String): Pair<Info, List<String>> {
                try {
                    val baseLink = celcatLinkPattern.find(link)?.value
                    val fids = extractFids(link.toUri())

                    if (baseLink.isNullOrBlank()) {
                        throw InvalidLinkException(R.string.error_invalid_link)
                    }

                    if (fids.isEmpty()) {
                        throw InvalidLinkException(R.string.error_link_groups)
                    }

                    val name = ""
                    val url = celcatLinkPattern.find(link)?.groups?.get(1)?.value!!
                    val groups = guessGroupsLink(url)
                    val rooms = guessRoomsLink(url)
                    val courses = guessCoursesLink(url)

                    return Pair(Info(name, url, groups, rooms, courses), fids)
                } catch (e: UnsupportedOperationException) {
                    throw InvalidLinkException(R.string.error_invalid_link)
                }
            }

            /**
             * Tries to guess the link to retrieve all the groups.
             *
             * @param link The base link
             * @return The group link
             */
            private fun guessGroupsLink(link: String): String {
                val search =
                    if (link.contains("calendar")) { "___" }
                    else { "__" }

                return "$link/Home/ReadResourceListItems?myResources=false&searchTerm=$search&pageSize=100000&pageNumber=1&resType=103"
            }

            /**
             * Tries to guess the link to retrieve all the rooms.
             *
             * @param link The base link
             * @return The rooms link
             */
            private fun guessRoomsLink(link: String): String {
                val search =
                    if (link.contains("calendar")) { "___" }
                    else { "__" }

                return "$link/Home/ReadResourceListItems?myResources=false&searchTerm=$search&pageSize=1000000&pageNumber=1&resType=102"
            }

            /**
             * Tries to guess the link to retrieve all the courses.
             *
             * @param link The base link
             * @return The courses link
             */
            private fun guessCoursesLink(link: String): String {
                val search =
                    if (link.contains("calendar")) { "___" }
                    else { "__" }

                return "$link/Home/ReadResourceListItems?myResources=false&searchTerm=$search&pageSize=10000000&pageNumber=1&resType=100"
            }
        }

        /**
         * Thrown if the given link is invalid.
         *
         * @property reason A resource id pointing to
         * the current error. (Can be used to display
         * errors to the final user ).
         */
        class InvalidLinkException(val reason: Int): Exception()

        /**
         * Represent a faculty group.
         *
         * @property id The group id
         * @property text The textual representation
         */
        @Serializable
        data class Group (
            val id: String,
            val text: String
        )
    }
}