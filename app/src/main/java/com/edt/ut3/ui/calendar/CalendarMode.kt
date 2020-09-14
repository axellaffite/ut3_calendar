package com.edt.ut3.ui.calendar

import org.json.JSONException
import org.json.JSONObject

data class CalendarMode(
    var mode: Mode = Mode.AGENDA,
    var forceWeek: Boolean = false)
{

    enum class Mode { AGENDA, WEEK }

    companion object {
        fun default() = CalendarMode(Mode.AGENDA, false)
        fun defaultWeek() = CalendarMode(Mode.WEEK, false)

        @Throws(JSONException::class)
        fun fromJson(str: String) : CalendarMode {
            val json = JSONObject(str)

            return CalendarMode(
                mode = Mode.valueOf(json.getString("mode")),
                forceWeek = json.getBoolean("forceWeek")
            )
        }
    }

    fun invertForceWeek() = CalendarMode(mode, !forceWeek)

    fun withForcedDay() = CalendarMode(mode, true)

    fun withoutForceDay() = CalendarMode(mode, false)

    fun withWeekMode() = CalendarMode(Mode.WEEK, forceWeek)

    fun withAgendaMode() = CalendarMode(Mode.AGENDA, forceWeek)


    fun toJSON(): String = JSONObject().apply {
        put("mode", mode.toString())
        put("forceWeek", forceWeek)
    }.toString()

}