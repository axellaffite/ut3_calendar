package com.edt.ut3.backend.requests.celcat

import android.content.Context
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.requests.JsonWebDeserializer
import com.edt.ut3.backend.requests.authentication_services.Authenticator
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.*

@Suppress("BlockingMethodInNonBlockingContext")
object CelcatDataRetriever {

    /**
     * Parse the events retrieved by the CelcatService
     * into a list of [events][Event]
     *
     * @param context A valid application context
     * @param firstUpdate Whether it is the first time
     * the events will be downloaded
     * @param link The link where the data should be retrieved
     * @param groups The groups to which the user is affiliated
     * @param classes The classes names, used to parse the [Events][Event]
     * @param courses The courses names, used to parse the [Events][Event]
     * @return A list of [events][Event].
     *
     * @throws SocketTimeoutException When the download timeout
     * has been reached
     * @throws IOException When the returned data is invalid
     * @throws Authenticator.InvalidCredentialsException When the
     * user's credentials are invalid
     * @throws SerializationException When the returned data
     * cannot be parsed with the current configuration
     */
    @Throws(
        Authenticator.InvalidCredentialsException::class,
        IOException::class,
        SocketTimeoutException::class,
        SerializationException::class
    )
    suspend fun getEvents(
        context: Context,
        firstUpdate: Boolean,
        link: School.Info,
        groups: List<String>,
        classes: HashSet<String>,
        courses: Map<String, String>
    ): List<Event> {
        val body = withContext(IO) {
            val response = CelcatService.getEvents(context, firstUpdate, link.url, groups)
            response.body?.string() ?: throw IOException()
        }


        return withContext(Default) {
            Json.parseToJsonElement(body).jsonArray.fold(listOf()) { acc, e ->
                try {
                    acc + Event.fromJSON(e.jsonObject, classes, courses)
                } catch (e: Exception) {
                    e.printStackTrace()
                    acc
                }
            }
        }
    }


    /**
     * Parses the classes names retrieved by the [CelcatService]
     * into a list of [String].
     *
     * @param context A valid application context
     * @param link The link from which the classes should be retrieved
     * @return A list of classes represented by a list of String.
     *
     * @throws SocketTimeoutException When the download timeout
     * has been reached
     * @throws IOException When the returned data is invalid
     * @throws Authenticator.InvalidCredentialsException When the
     * user's credentials are invalid
     * @throws SerializationException When the returned data
     * cannot be parsed with the current configuration
     */
    @Throws(
        IOException::class,
        SocketTimeoutException::class,
        Authenticator.InvalidCredentialsException::class,
        SerializationException::class
    )
    suspend fun getClasses(context: Context, link: String): List<String> {
        val body = withContext(IO) {
            val response = CelcatService.getClasses(context, link)
            response.body?.string() ?: throw IOException()
        }

        return withContext(Default) {
            JsonWebDeserializer.decodeFromString<ClassesRequest>(body).results
        }
    }


    /**
     * Parse the [schools information][School] retrieved by [CelcatService].
     *
     * @return The school information retrieved by [CelcatService]
     * as a list of [School].
     *
     * @throws SocketTimeoutException When the download timeout
     * has been reached
     * @throws IOException When the returned data is invalid
     * @throws SerializationException When the returned data
     * cannot be parsed with the current configuration
     */
    @Throws(
        SocketTimeoutException::class,
        IOException::class,
        SerializationException::class
    )
    suspend fun getSchoolsURLs(): List<School> {
        val body = withContext(IO) {
            val response = CelcatService.getSchoolsURLs()
            response.body?.string() ?: throw IOException()
        }

        return withContext(Default) {
            JsonWebDeserializer.decodeFromString<SchoolsRequest>(body).entries
        }
    }


    /**
     * Parse the courses names retrieved by the [CelcatService]
     * into a map of String.
     * The courses are mapped in the following way :
     * As an courses are composed by an ID and a Textual representation,
     * each id is mapped to the textual representation and vice versa.
     *
     * @param context A valid application context
     * @param link The link from which the courses names should
     * be retrieved
     * @return A Map of courses names represented as a Map<String, String>
     *
     * @throws SocketTimeoutException When the download timeout
     * has been reached
     * @throws IOException When the returned data is invalid
     * @throws SerializationException When the returned data
     * cannot be parsed with the current configuration
     */
    @Throws(
        IOException::class,
        SocketTimeoutException::class,
        SerializationException::class
    )
    suspend fun getCoursesNames(context: Context, link: String): Map<String, String> {
        val body = withContext(IO) {
            val response = CelcatService.getCoursesNames(context, link)
            response.body?.string() ?: throw IOException()
        }

        return withContext(Default) {
            JsonWebDeserializer.decodeFromString<CoursesRequest>(body).results.toMap()
        }
    }


    /**
     * Parse the groups retrieved by the [CelcatService]
     * into a list of [groups][School.Info.Group].
     *
     * @param context A valid application context
     * @param url The url from where the groups should be retrieve
     * @return A list of [groups][School.Info.Group]
     *
     * @throws SocketTimeoutException When the download timeout
     * has been reached
     * @throws IOException When the returned data is invalid
     * @throws Authenticator.InvalidCredentialsException When the
     * user's credentials are invalid
     * @throws SerializationException When the returned data
     * cannot be parsed with the current configuration
     */
    @Throws(
        SocketTimeoutException::class,
        IOException::class,
        Authenticator.InvalidCredentialsException::class,
        SerializationException::class
    )
    suspend fun getGroups(context: Context, url: String): List<School.Info.Group> {
        val body = withContext(IO) {
            val response = CelcatService.getGroups(context, url)
            response.body?.string() ?: throw IOException()
        }

        return withContext(Default) {
            JsonWebDeserializer.decodeFromString<GroupsRequest>(body).results
        }
    }

}