package com.edt.ut3.backend.background_services.updaters

import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.requests.celcat.CelcatService
import com.edt.ut3.misc.extensions.minus
import com.edt.ut3.misc.extensions.timeCleaned
import io.ktor.client.*
import java.util.*

class CelcatUpdater(client: HttpClient) : Updater {

    private val service = CelcatService(client)

    override suspend fun getClasses(link: String): List<String> {
        return service.getClasses(link)
    }

    override suspend fun getCoursesNames(link: String): Map<String, String> {
        return service.getCoursesNames(link)
    }

    override suspend fun getEvents(
        link: School.Info,
        resourceType: ResourceType,
        groups: List<String>,
        classes: Set<String>,
        courses: Map<String, String>,
        isFirstUpdate: Boolean
    ): List<Event> {
        val today = Date().timeCleaned()
        val startDate = if (isFirstUpdate) today.minus(Calendar.YEAR, 1) else today

        return service.getEvents(
            link.url,
            start = startDate,
            resType = resourceType,
            groups,
            classes,
            courses
        )
    }
}