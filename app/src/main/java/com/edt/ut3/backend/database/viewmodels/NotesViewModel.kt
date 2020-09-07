package com.edt.ut3.backend.database.viewmodels

import android.content.Context
import com.edt.ut3.backend.database.AppDatabase

class NotesViewModel(context: Context) {

    private val dao = AppDatabase.getInstance(context).noteDao()

    fun getNotesLD() = dao.selectAllLD()

}