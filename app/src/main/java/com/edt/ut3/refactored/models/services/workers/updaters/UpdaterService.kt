package com.edt.ut3.refactored.models.services.workers.updaters

import com.edt.ut3.refactored.models.domain.celcat.Event
import com.edt.ut3.backend.formation_choice.School
import io.ktor.client.*

interface UpdaterService {
    suspend fun getClasses(client: HttpClient, link: String): List<String>
    suspend fun getCoursesNames(client: HttpClient, link: String): Map<String, String>
    suspend fun getEvents(
        client: HttpClient,
        link: School.Info,
        groups: List<String>,
        classes: Set<String>,
        courses: Map<String, String>,
        isFirstUpdate: Boolean
    ): List<Event>
}