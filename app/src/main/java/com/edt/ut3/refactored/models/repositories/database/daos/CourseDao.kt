package com.edt.ut3.refactored.models.repositories.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.edt.ut3.refactored.models.domain.celcat.Course

@Dao
interface CourseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg courses: Course)

    @Query("SELECT * FROM course ORDER BY title")
    suspend fun selectAll(): List<Course>

    @Query("SELECT * FROM course ORDER BY title")
    fun selectAllLD(): LiveData<List<Course>>

    @Query("DELETE FROM course where title in (:titles)")
    suspend fun remove(vararg titles: String)

}