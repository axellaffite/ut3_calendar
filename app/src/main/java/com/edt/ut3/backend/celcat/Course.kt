package com.edt.ut3.backend.celcat

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "course")
data class Course(
    @PrimaryKey var title : String,
    var visible : Boolean = true
)