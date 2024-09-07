package com.edt.ut3.backend.formation_choice

import android.net.Uri
import com.edt.ut3.backend.background_services.updaters.ResourceType
import com.edt.ut3.backend.formation_choice.School.Info
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AuthenticationMethod{
    @SerialName("none")
    NONE,
    @SerialName("ut3_fsi")
    UT3_FSI
}

/**
 * Represents a School by its name and [information][Info].
 *
 * @property name
 * @property info
 */
@Serializable
data class School(
    val name: String,
    val info: Info
){

    /**
     * Represent a [school][School] information.
     *
     * @property label The label associated wih the faculty
     * @property baseUrl The base CELCAT url
     * @property authentication The authentication method used by the CELCAT instance
     * @property searchPlaceHolder The placeholder used in search queries to get all the entries
     */
    @Serializable
    data class Info (
        val label: String,
        val baseUrl: String,
        val authentication: AuthenticationMethod,
        val searchPlaceHolder: String
    ){
        fun get(resourceType: ResourceType) = when (resourceType) {
            ResourceType.Groups -> getGroupLink(baseUrl, searchPlaceHolder)
            ResourceType.Courses -> getCoursesLink(baseUrl, searchPlaceHolder)
            ResourceType.Classes -> getRoomsLink(baseUrl, searchPlaceHolder)
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

            /**
             * Returns the link to retrieve all the groups.
             *
             * @param link The base link
             * @param searchPlaceHolder The string used as placeholder to fetch all entries (varies between CELCAT instances)
             * @return The group link
             */
            private fun getGroupLink(link: String, searchPlaceHolder: String): String {
                return "$link/Home/ReadResourceListItems?myResources=false&searchTerm=$searchPlaceHolder&pageSize=100000&pageNumber=1&resType=103"
            }

            /**
             * Tries to guess the link to retrieve all the rooms.
             *
             * @param link The base link
             * @param searchPlaceHolder The string used as placeholder to fetch all entries (varies between CELCAT instances)
             * @return The rooms link
             */
            private fun getRoomsLink(link: String, searchPlaceHolder: String): String {
                return "$link/Home/ReadResourceListItems?myResources=false&searchTerm=$searchPlaceHolder&pageSize=1000000&pageNumber=1&resType=102"
            }

            /**
             * Tries to guess the link to retrieve all the courses.
             *
             * @param link The base link
             * @return The courses link
             */
            private fun getCoursesLink(link: String, searchPlaceHolder: String): String {
                return "$link/Home/ReadResourceListItems?myResources=false&searchTerm=$searchPlaceHolder&pageSize=10000000&pageNumber=1&resType=100"
            }
        }

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