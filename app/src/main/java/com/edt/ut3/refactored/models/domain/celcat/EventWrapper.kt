package com.edt.ut3.refactored.models.domain.celcat

import com.elzozor.yoda.events.EventWrapper

/**
 * Wrapper is used to wrap an event in order to build the views
 * and parse them with the yoda library.
 *
 * @property event The event to encapsulate.
 */
class EventWrapper(val event: Event) : EventWrapper() {
    override fun begin() = event.start

    override fun end() = event.end ?: event.start

    override fun isAllDay() = event.allday
}