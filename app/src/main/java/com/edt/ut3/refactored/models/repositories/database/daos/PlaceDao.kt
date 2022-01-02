package com.edt.ut3.refactored.models.repositories.database.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.edt.ut3.refactored.models.domain.maps.Place

@Dao
interface PlaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg placeInfo: Place)

    @Delete
    suspend fun delete(vararg placeInfo: Place)

    @Query("SELECT * FROM place_info")
    suspend fun selectAll() : List<Place>

    @Query("SELECT * FROM place_info")
    fun selectAllLD() : LiveData<List<Place>>

}