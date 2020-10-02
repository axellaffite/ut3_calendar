package com.edt.ut3.ui.preferences.formation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.edt.ut3.backend.formation_choice.SchoolURL
import com.edt.ut3.backend.requests.CelcatService
import org.json.JSONException
import java.io.IOException

class FormationChoiceViewModel: ViewModel() {

    val currentFragment = MutableLiveData(0)
    private var schoolURLs = listOf<SchoolURL>()

    @Synchronized
    @Throws(IOException::class, JSONException::class)
    suspend fun getFormations(): List<SchoolURL> {
        if (schoolURLs.isNullOrEmpty()) {
            schoolURLs = CelcatService().getSchoolsURLs() ?: throw IOException()
        }

        return schoolURLs
    }

}