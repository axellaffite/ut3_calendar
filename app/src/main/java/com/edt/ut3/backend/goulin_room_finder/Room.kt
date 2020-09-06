package com.edt.ut3.backend.goulin_room_finder

data class Room (
    val building: String,
    val freeSchedules: List<Schedule>,
    val room: String
)