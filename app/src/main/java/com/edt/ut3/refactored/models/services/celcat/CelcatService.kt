package com.edt.ut3.refactored.models.services.celcat

import com.edt.ut3.refactored.models.domain.celcat.Event
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.misc.extensions.toCelcatDateStr
import com.elzozor.yoda.utils.DateExtensions.add
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import java.util.*

private const val schoolsUrl = "https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/data/formations/urls.json"

class CelcatService {
    private inline fun Parameters.Companion.buildCelcatParameters(init: ParametersBuilder.() -> Unit): Parameters {
        return ParametersBuilder().apply {
            append("resType", "103")
            append("calView", "agendaDay")
            append("colourScheme", "3")
            init()
        }.build()
    }

    suspend fun getEvents(
        client: HttpClient,
        link: String,
        start: Date,
        formations: List<String>,
        classes: Set<String>,
        courses: Map<String, String>
    ): List<Event> {
        val events: JsonArray = client.submitForm(
            url = "$link/Home/GetCalendarData",
            formParameters = Parameters.buildCelcatParameters {
                append("start", start.toCelcatDateStr())
                append("end", Date().add(Calendar.YEAR, 1).toCelcatDateStr())

                formations.forEach { append("federationIds[]", it) }
            },
            block = {
                header(HttpHeaders.Accept, ContentType.Application.Json)
            }
        )

        return events.map { CelcatConverter().fromJSON(it.jsonObject, classes, courses) }
    }

    suspend fun getClasses(client: HttpClient, link: String): List<String> {
        return client.get<ClassesRequest>(link) {
            header(HttpHeaders.Accept, ContentType.Application.Json.withCharset(Charsets.UTF_8))
        }.results
    }

    suspend fun getCoursesNames(client: HttpClient, link: String): Map<String, String> {
        return client.get<CoursesRequest>(link).results
    }

    suspend fun getSchoolsURLs(client: HttpClient): List<School> {
        return client.get<SchoolsRequest>(schoolsUrl).entries
    }

    suspend fun getGroups(client: HttpClient, link: String): List<School.Info.Group> {
        return client.get<GroupsRequest>(link).results
    }

}
