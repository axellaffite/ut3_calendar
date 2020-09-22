package com.edt.ut3.backend.goulin_room_finder

import java.util.*

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