package com.elzozor.ut3calendar.backend.database.daos

import androidx.room.*
import com.elzozor.ut3calendar.backend.celcat.Event

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg events: Event)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg events: Event)

    @Delete
    suspend fun delete(vararg events: Event)

    @Query("DELETE FROM event")
    suspend fun wipe()

    @Query("SELECT * FROM event")
    suspend fun selectAll(): List<Event>

    @Query("SELECT * FROM event WHERE id in (:ids)")
    suspend fun selectByIDs(vararg ids: String): List<Event>

    @Query("SELECT * FROM event WHERE :noteIDs in (:noteIDs)")
    suspend fun selectByNotesIDs(noteIDs: Long): List<Event>

    @Query("SELECT * FROM event WHERE start BETWEEN :start AND :end")
    suspend fun getFromTo(start: Long, end: Long): List<Event>
}