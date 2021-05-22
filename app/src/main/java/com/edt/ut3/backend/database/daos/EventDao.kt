package com.edt.ut3.backend.database.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.edt.ut3.backend.celcat.Event

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg events: Event)

    @Update
    suspend fun update(vararg events: Event)

    @Delete
    suspend fun delete(vararg events: Event)

    @Query("DELETE FROM event where id = :eventID")
    suspend fun deleteID(eventID: String)

    @Query("DELETE FROM event")
    suspend fun wipe()

    @Query("SELECT * FROM event")
    suspend fun selectAll(): List<Event>

    @Query("SELECT * FROM event")
    fun selectAllLD(): LiveData<List<Event>>

    @Query("SELECT * FROM event WHERE id in (:ids)")
    suspend fun selectByIDs(vararg ids: String): List<Event>

    @Query("SELECT * FROM event WHERE id in (:ids)")
    fun selectByIDsLD(vararg ids: String): LiveData<List<Event>>

    @Query("SELECT * FROM event WHERE :noteIDs in (:noteIDs)")
    suspend fun selectByNotesIDs(noteIDs: Long): List<Event>

    @Query("SELECT * FROM event WHERE start BETWEEN :start AND :end")
    suspend fun getFromTo(start: Long, end: Long): List<Event>

    @Query("SELECT * FROM event WHERE start BETWEEN :start AND :end")
    fun getFromToLD(start: Long, end: Long): LiveData<List<Event>>

    @Query("SELECT DISTINCT courseName FROM event")
    suspend fun getCourses() : List<String?>
}