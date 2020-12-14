package com.edt.ut3.backend.calendar

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Test class for [CalendarMode]
 */
class CalendarModeTest {

    private val agenda = CalendarMode.default()
    private val agendaForced = CalendarMode.default().withForcedWeek()
    private val week = CalendarMode.defaultWeek()
    private val weekForced = CalendarMode.defaultWeek().withForcedWeek()

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun isAgenda() {
        assertTrue(agenda.isAgenda())
        assertFalse(week.isAgenda())
    }

    @Test
    fun isWeek() {
        assertFalse(agenda.isWeek())
        assertTrue(week.isWeek())
    }

    @Test
    fun invertForceWeek() {
        assertFalse(agenda.forceWeek)
        assertFalse(week.forceWeek)

        val agendaInvertedForce = agenda.invertForceWeek()
        val weekInvertedForce = week.invertForceWeek()
        assertTrue(agendaInvertedForce.isWeek())
        assertTrue(weekInvertedForce.isWeek())
        assertTrue(agendaInvertedForce.forceWeek)
        assertTrue(weekInvertedForce.forceWeek)
    }

    @Test
    fun withForcedWeek() {
        assertFalse(agenda.forceWeek)
        assertFalse(week.forceWeek)

        val agendaForcedWeek = agenda.withForcedWeek()
        val weekForcedWeek = week.withForcedWeek()
        assertTrue(agendaForcedWeek.forceWeek)
        assertTrue(agendaForcedWeek.isWeek())
        assertTrue(weekForcedWeek.forceWeek)
        assertTrue(weekForcedWeek.isWeek())
    }

    @Test
    fun withoutForcedWeek() {
        assertFalse(agenda.forceWeek)
        assertFalse(week.forceWeek)

        val agendaForcedWeek = agenda.withoutForcedWeek()
        val weekForcedWeek = week.withoutForcedWeek()
        assertFalse(agendaForcedWeek.forceWeek)
        assertFalse(agendaForcedWeek.isWeek())
        assertFalse(weekForcedWeek.forceWeek)
        assertTrue(weekForcedWeek.isWeek())
    }

    @Test
    fun withWeekMode() {
        assertFalse(agenda.isWeek())
        assertTrue(week.isWeek())

        assertTrue(agenda.withWeekMode().isWeek())
        assertTrue(week.withWeekMode().isWeek())
    }

    @Test
    fun withAgendaMode() {
        assertFalse(agenda.isWeek())
        assertTrue(week.isWeek())

        assertFalse(agenda.withAgendaMode().isWeek())
        assertFalse(week.withAgendaMode().isWeek())
        assertTrue(agendaForced.withAgendaMode().isWeek())
        assertTrue(weekForced.withAgendaMode().isWeek())
    }

    @Test
    fun getMode() {
        assertEquals(CalendarMode.Mode.AGENDA, agenda.mode)
        assertEquals(CalendarMode.Mode.AGENDA, agendaForced.mode)
        assertEquals(CalendarMode.Mode.WEEK, week.mode)
        assertEquals(CalendarMode.Mode.WEEK, weekForced.mode)
    }

    @Test
    fun getForceWeek() {
        assertFalse(agenda.forceWeek)
        assertFalse(week.forceWeek)
        assertTrue(agendaForced.forceWeek)
        assertTrue(weekForced.forceWeek)
    }

}