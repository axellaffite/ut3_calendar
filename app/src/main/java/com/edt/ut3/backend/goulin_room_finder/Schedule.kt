package com.edt.ut3.backend.goulin_room_finder

import java.util.*

/**
 * Represents a schedule in
 * the Goulin's API.
 *
 * @property start The beginning of the schedule
 * @property end The ending of the schedule
 */
data class Schedule(val start: Date, val end: Date)