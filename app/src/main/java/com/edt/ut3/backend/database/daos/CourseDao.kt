package com.edt.ut3.backend.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.edt.ut3.backend.celcat.Course
import com.edt.ut3.backend.celcat.CourseStatusData
import java.util.*

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

    @Query("""
        SELECT 
            course.title AS title, 
            course.visible AS visible, 
            COUNT(case when event.`end` > :currentTimestamp then 1 else null end) AS remaining 
        FROM course, event 
        WHERE event.courseName = course.title 
        GROUP BY course.title, course.visible 
        ORDER BY remaining = 0, title"""
    )
    fun selectAllLDWithRemaining(currentTimestamp: Long = Date().time): LiveData<List<CourseStatusData>>

}