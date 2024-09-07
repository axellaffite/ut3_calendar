package com.edt.ut3.backend.background_services.updaters

/**
 * Resource types than can be fetched from the Celcat's API.
 * This is done to match the new system of the university (for flex licences).
 *
 * @property uiChoice position of the choice on the UI. This position corresponds
 * to the resource defined in string resources (R.string.resource_type_possibilities).
 * @property resType corresponding resType argument sent to Celcat's API.
 */
enum class ResourceType(val uiChoice: Int, val resType: String) {
    Groups(uiChoice = 0, resType = "103"),
    Courses(uiChoice = 1, resType = "100"),
    Classes(uiChoice = 2, resType = "102");

    companion object {
        fun fromUiSource(uiChoice: Int) =
            values().firstOrNull { it.uiChoice == uiChoice } ?: Groups
    }
}