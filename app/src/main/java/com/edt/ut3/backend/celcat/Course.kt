package com.edt.ut3.backend.celcat

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Used to store and set the
 * visibility on different courses.
 *
 * @property title The course title
 * @property visible Whether the course is visible
 */
@Entity(tableName = "course")
data class Course(
    @PrimaryKey var title : String,
    var visible : Boolean = true
)

/**
 * Used to set the data
 * on the current View.
 *
 * @property title The course title
 * @property remaining The amount of remaining
 * lessons from the current date.
 */
data class CourseStatusData (
    val title : String,
    val visible : Boolean,
    val remaining: Int
)