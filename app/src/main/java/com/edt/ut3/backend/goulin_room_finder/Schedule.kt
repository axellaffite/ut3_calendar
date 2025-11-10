package com.edt.ut3.backend.goulin_room_finder


import com.fasterxml.jackson.annotation.JsonFormat
import java.util.Date

/**
 * Represents a schedule in
 * the Goulin's API.
 *
 * @property start The beginning of the schedule
 * @property end The ending of the schedule
 */

data class Schedule(
    @field:JsonFormat(timezone = "PST")
    val start: Date,
    @field:JsonFormat(timezone = "PST")
    val end: Date
)
