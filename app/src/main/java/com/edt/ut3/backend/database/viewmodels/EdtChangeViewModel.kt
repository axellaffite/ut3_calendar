package com.edt.ut3.backend.database.viewmodels

import android.content.Context
import com.edt.ut3.backend.database.AppDatabase
import com.edt.ut3.backend.notification.EventChange

class EdtChangeViewModel(context: Context) {

    val dao = AppDatabase.getInstance(context).edtChangeDao()

    /**
     * Insert all the given eventChanges into the database.
     * @param eventChange
     */
    suspend fun insert(vararg eventChange: EventChange) = dao.insert(*eventChange)

}