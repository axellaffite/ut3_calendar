package com.edt.ut3.refactored.models.repositories.database.daos

import androidx.room.*
import com.edt.ut3.refactored.models.domain.notifications.EventChange

@Dao
interface EdtChangeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg ev: EventChange): List<Long>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(ev: EventChange)

    @Delete
    suspend fun delete(ev: EventChange)

    @Query("DELETE FROM note")
    suspend fun wipe()

    @Query("SELECT * FROM event_change ORDER BY DATE ASC")
    suspend fun selectAll(): List<EventChange>


}