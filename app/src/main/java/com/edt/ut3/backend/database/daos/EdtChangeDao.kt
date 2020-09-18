package com.edt.ut3.backend.database.daos

import androidx.room.*
import com.edt.ut3.backend.notification.EventChange

@Dao
interface EdtChangeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ev: EventChange): List<Long>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(ev: EventChange)

    @Delete
    suspend fun delete(ev: EventChange)

    @Query("DELETE FROM note")
    suspend fun wipe()

    @Query("SELECT * FROM note ORDER BY DATE ASC")
    suspend fun selectAll(): List<EventChange>


}