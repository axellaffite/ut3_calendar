package com.edt.ut3.backend.database.viewmodels

import android.content.Context
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.notification.EventChange

class EdtChangeViewModel(context: Context) {

    val dao = AppDatabase.getInstance(context).edtChangeDao()

    suspend fun insert(vararg eventChange: EventChange) = dao.insert(*eventChange)

}