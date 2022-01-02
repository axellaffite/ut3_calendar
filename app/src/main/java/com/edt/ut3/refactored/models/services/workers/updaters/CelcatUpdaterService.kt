package com.edt.ut3.refactored.models.services.workers.updaters

import com.edt.ut3.refactored.models.domain.celcat.Event
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.refactored.models.services.celcat.CelcatService
import com.edt.ut3.misc.extensions.minus
import com.edt.ut3.misc.extensions.timeCleaned
import io.ktor.client.*
import java.util.*

class CelcatUpdaterService(
    private val service: CelcatService
) : UpdaterService {
    override suspend fun getClasses(client: HttpClient, link: String): List<String> {
        return service.getClasses(client, link)
    }

    override suspend fun getCoursesNames(client: HttpClient, link: String): Map<String, String> {
        return service.getCoursesNames(client, link)
    }

    override suspend fun getEvents(
        client: HttpClient,
        link: School.Info,
        groups: List<String>,
        classes: Set<String>,
        courses: Map<String, String>,
        isFirstUpdate: Boolean
    ): List<Event> {
        val today = Date().timeCleaned()
        val startDate = if (isFirstUpdate) today.minus(Calendar.YEAR, 1) else today

        return service.getEvents(
            client = client,
            link = link.url,
            start = startDate,
            formations = groups,
            classes = classes,
            courses = courses
        )
    }
}