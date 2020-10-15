package com.edt.ut3.ui.preferences.formation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.requests.CelcatService
import com.edt.ut3.backend.requests.authentication_services.Authenticator
import com.edt.ut3.misc.Optional
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.io.IOException

class FormationViewModel: ViewModel() {

    var credentials = MutableLiveData(Authenticator.Credentials.from(null, null))
    val availableGroups = mutableListOf<School.Info.Group>()
    val school = MutableLiveData<Optional<School?>>(Optional.of(School.default))
    val link = MutableLiveData<Optional<School.Info?>>(Optional.of(School.default.info.first()))
    val groups = MutableLiveData<Set<School.Info.Group>>(setOf())

    fun reset() {
        school.value = Optional.empty()
        link.value = Optional.empty()
    }

    private val schools = mutableListOf<School>()

    private val schoolsMutex = Mutex()
    @Throws(IOException::class, JSONException::class)
    suspend fun getSchools(): List<School> = withContext(IO) {
        schoolsMutex.withLock {
            if (schools.isNullOrEmpty()) {
                schools.addAll(CelcatService().getSchoolsURLs())
            }
        }

        schools
    }

}