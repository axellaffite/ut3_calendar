package com.edt.ut3.backend.calendar

import com.edt.ut3.backend.calendar.CalendarMode.Mode
import kotlinx.serialization.Serializable

/**
 * Used to save the current CalendarMode
 * configuration.
 *
 * @property mode Indicate the current
 * calendar mode.
 * @property forceWeek If true, this force
 * the Calendar to be displayed in [Mode.WEEK] mode
 * event if it's in portrait.
 */
@Serializable
data class CalendarMode(
    val mode: Mode = Mode.AGENDA,
    val forceWeek: Boolean = false
) {

    enum class Mode { AGENDA, WEEK }

    companion object {
        /**
         * Returns a default version of
         * the [CalendarMode] which is
         * in [AGENDA][Mode.AGENDA] mode and where the
         * week mode isn't forced.
         */
        fun default() = CalendarMode(Mode.AGENDA, false)

        /**
         * Returns a default version of
         * the [CalendarMode] which is
         * in [WEEK][Mode.WEEK] mode and where the week
         * mode isn't forced.
         */
        fun defaultWeek() = CalendarMode(Mode.WEEK, false)
    }

    /**
     * Returns true if the week mode isn't
     * forced and if the mode is equal to [AGENDA][Mode.AGENDA]).
     */
    fun isAgenda() = (!forceWeek && mode == Mode.AGENDA)

    /**
     * Negation of the [isAgenda] function
     */
    fun isWeek() = (!isAgenda())

    /**
     * Returns a [CalendarMode] object with
     * the current configuration but the [forceWeek]
     * variable is negated.
     */
    fun invertForceWeek() = CalendarMode(mode, !forceWeek)

    /**
     * Returns a [CalendarMode] object with
     * the current configuration but the [forceWeek]
     * variable is set to true.
     */
    fun withForcedWeek() = CalendarMode(mode, true)

    /**
     * Returns a [CalendarMode] object with
     * the current configuration but the [forceWeek]
     * variable is set to false.
     */
    fun withoutForcedWeek() = CalendarMode(mode, false)

    /**
     * Returns a [CalendarMode] object with
     * the current configuration but the [mode]
     * variable is set to [Mode.WEEK].
     */
    fun withWeekMode() = CalendarMode(Mode.WEEK, forceWeek)

    /**
     * Returns a [CalendarMode] object with
     * the current configuration but the [mode]
     * variable is set to [Mode.AGENDA].
     */
    fun withAgendaMode() = CalendarMode(Mode.AGENDA, forceWeek)

}