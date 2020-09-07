package com.edt.ut3.backend.database.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import com.edt.ut3.backend.celcat.Course
import com.edt.ut3.backend.database.AppDatabase

class CoursesViewModel(context: Context): ViewModel() {

    private val dao = AppDatabase.getInstance(context).courseDao()

    fun getCoursesLD() = dao.selectAllLD()

    suspend fun getCoursesVisibility() = dao.selectAll()

    suspend fun insert(vararg courses: Course) = dao.insert(*courses)

    suspend fun remove(vararg titles: String) = dao.remove(*titles)

}