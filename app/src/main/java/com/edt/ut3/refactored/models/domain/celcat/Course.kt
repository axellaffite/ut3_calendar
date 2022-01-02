package com.edt.ut3.refactored.models.domain.celcat

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