package com.edt.ut3.backend.goulin_room_finder

import kotlinx.serialization.Serializable


/**
 * Represents a Building
 * for the Goulin's API.
 *
 * @property name
 */
@Serializable
data class Building(val name: String)