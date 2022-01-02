package com.edt.ut3.refactored.models.domain

import androidx.room.Embedded
import androidx.room.Relation
import com.edt.ut3.backend.note.Note
import com.edt.ut3.refactored.models.domain.celcat.Event

data class EventWithNote(
    @Embedded val event: Event?,
    @Relation(parentColumn = "id", entityColumn = "event_id") val note: Note?
)