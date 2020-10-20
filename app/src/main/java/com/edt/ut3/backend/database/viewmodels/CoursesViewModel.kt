package com.edt.ut3.backend.database.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.edt.ut3.backend.celcat.Course
import com.edt.ut3.backend.database.AppDatabase

class CoursesViewModel(context: Context): ViewModel() {

    private val dao = AppDatabase.getInstance(context).courseDao()

    /**
     * Returns all the courses into a LiveData variable.
     */
    fun getCoursesLD() = dao.selectAllLD()

    /**
     * Returns all the courses visibilities.
     */
    suspend fun getCoursesVisibility() = dao.selectAll()

    /**
     * Insert all the given courses into the database.
     */
    suspend fun insert(vararg courses: Course) = dao.insert(*courses)

    /**
     * Remove all the given courses from the database.
     */
    suspend fun remove(vararg titles: String) = dao.remove(*titles)

}