package com.edt.ut3.refactored.models.domain.room_finder

import kotlinx.serialization.Serializable
import java.util.*

/**
 * Represents a room in
 * the Goulin's API.
 *
 * @property building The associated building
 * @property freeSchedules The schedules for which
 * the room is free
 * @property room The room's name
 */
@Serializable
data class Room (
    val building: String,
    val freeSchedules: List<Schedule>,
    val room: String)
{
    fun withoutPastSchedules(limitDate: Date) = Room (
        building,
        freeSchedules.filter { it.end > limitDate },
        room
    )
}