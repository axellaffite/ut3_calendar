package com.edt.ut3.refactored.viewmodels

import androidx.lifecycle.ViewModel
import com.edt.ut3.refactored.models.domain.notifications.EventChange
import com.edt.ut3.refactored.models.repositories.database.AppDatabase

class EdtChangeViewModel(database: AppDatabase): ViewModel() {

    val dao = database.edtChangeDao()

    /**
     * Insert all the given eventChanges into the database.
     * @param eventChange
     */
    suspend fun insert(vararg eventChange: EventChange) = dao.insert(*eventChange)

}