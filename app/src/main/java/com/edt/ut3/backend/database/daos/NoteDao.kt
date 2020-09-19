package com.edt.ut3.backend.database.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.edt.ut3.backend.note.Note


@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg notes: Note): List<Long>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(vararg notes: Note)

    @Delete
    suspend fun delete(vararg notes: Note)

    @Query("DELETE FROM note")
    suspend fun wipe()

    @Query("SELECT * FROM note")
    suspend fun selectAll(): List<Note>

    @Query("SELECT * FROM note ORDER BY date")
    fun selectAllLD(): LiveData<List<Note>>

    @Query("SELECT * FROM note WHERE id in (:ids)")
    suspend fun selectByIDs(vararg ids: Long): List<Note>

    @Query("SELECT * FROM note WHERE event_id in (:eventIDs)")
    suspend fun selectByEventIDs(vararg eventIDs: String): List<Note>

    @Query("SELECT * FROM note WHERE date BETWEEN :begin AND :end")
    suspend fun getFromTo(begin: Long, end: Long): List<Note>

}