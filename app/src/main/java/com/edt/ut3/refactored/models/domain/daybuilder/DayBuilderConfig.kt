package com.edt.ut3.refactored.models.domain.daybuilder

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.edt.ut3.refactored.models.domain.celcat.Course
import com.edt.ut3.refactored.models.domain.celcat.Event
import com.elzozor.yoda.events.EventWrapper
import java.util.*

interface DayBuilderConfig {
    val eventList: List<Event>
    val currentDate: Date
    val currentPosition: Int
    val dayPosition: Int
    val container: FrameLayout
    val courseVisibility: List<Course>
    val dayBuilder: (context: Context, eventWrapper: EventWrapper, x: Int, y: Int, w: Int, h: Int) -> Pair<Boolean, View>
    val emptyDayBuilder: () -> View
    val allDayBuilder: (events: List<EventWrapper>) -> View
    val height: Int
    val width: Int
}