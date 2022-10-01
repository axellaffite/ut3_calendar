package com.edt.ut3.backend.background_services.updaters

import com.edt.ut3.backend.celcat.Event
import com.edt.ut3.backend.formation_choice.School

interface Updater {
    suspend fun getClasses(link: String): List<String>
    suspend fun getCoursesNames(link: String): Map<String, String>
    suspend fun getEvents(
        link: School.Info,
        resourceType: ResourceType,
        groups: List<String>,
        classes: Set<String>,
        courses: Map<String, String>,
        isFirstUpdate: Boolean
    ): List<Event>
}