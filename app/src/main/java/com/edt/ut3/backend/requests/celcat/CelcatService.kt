package com.edt.ut3.backend.requests.celcat


import com.edt.ut3.backend.background_services.updaters.ResourceType
import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.celcat.RawEvent
import com.edt.ut3.backend.celcat.toParsedEvent
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.misc.extensions.toCelcatDateStr
import com.elzozor.yoda.utils.DateExtensions.add
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.withCharset
import java.util.Calendar
import java.util.Date

class CelcatService(val client: HttpClient) {

    private inline fun Parameters.Companion.buildCelcatParameters(
        resType: ResourceType,
        init: ParametersBuilder.() -> Unit
    ): Parameters {
        return ParametersBuilder().apply {
            append("resType", resType.resType)
            append("calView", "agendaDay")
            append("colourScheme", "3")
            init()
        }.build()
    }

    suspend fun getEvents(
        link: String,
        start: Date,
        resType: ResourceType,
        formations: List<String>,
        classes: Set<String>,
        courses: Map<String, String>
    ): List<Event> {
        val events: List<RawEvent> = client.submitForm(
            url = "$link/Home/GetCalendarData",
            formParameters = Parameters.buildCelcatParameters(resType = resType) {
                append("start", start.toCelcatDateStr())
                append("end", Date().add(Calendar.YEAR, 1).toCelcatDateStr())

                formations.forEach { append("federationIds[]", it) }
            },
            block = {
                header(HttpHeaders.Accept, ContentType.Application.Json)
            }
        ).body()

        return events.map { it.toParsedEvent(classes, courses) }
    }

    suspend fun getClasses(link: String): List<String> {
        return client.get(link) {
            header(HttpHeaders.Accept, ContentType.Application.Json.withCharset(Charsets.UTF_8))
        }.body<ClassesRequest>().results
    }

    suspend fun getCoursesNames(link: String): Map<String, String> {
        return client.get(link).body<CoursesRequest>().results
    }

    suspend fun getSchoolsURLs(): List<School> {
        return client.get(
            "https://raw.githubusercontent.com/axellaffite/ut3_calendar/master/data/formations/urls.json"
        ).body<SchoolsRequest>().entries
    }

    suspend fun getGroups(link: String): List<School.Info.Group> {
        return client.get(link).body<GroupsRequest>().results
    }

}

