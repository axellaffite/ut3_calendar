package com.edt.ut3.ui.preferences.formation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.requests.CelcatService
import org.json.JSONException
import java.io.IOException

class FormationChoiceViewModel: ViewModel() {

    var link = MutableLiveData<School.Info>()
    var school = MutableLiveData<School>()
    val currentFragment = MutableLiveData(0)
    private var schoolURLs = listOf<School>()

    @Synchronized
    @Throws(IOException::class, JSONException::class)
    suspend fun getFormations(): List<School> {
        if (schoolURLs.isNullOrEmpty()) {
            schoolURLs = CelcatService().getSchoolsURLs() ?: throw IOException()
        }

        return schoolURLs
    }

}