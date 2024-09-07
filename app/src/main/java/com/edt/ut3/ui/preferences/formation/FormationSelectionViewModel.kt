package com.edt.ut3.ui.preferences.formation

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.edt.ut3.backend.background_services.updaters.ResourceType
import com.edt.ut3.backend.credentials.CredentialsManager
import com.edt.ut3.backend.firebase_services.FirebaseMessagingHandler
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.backend.requests.authentication_services.AuthenticationException
import com.edt.ut3.backend.requests.authentication_services.Authenticator
import com.edt.ut3.backend.requests.authentication_services.Credentials
import com.edt.ut3.backend.requests.authentication_services.getAuthenticator
import com.edt.ut3.backend.requests.celcat.CelcatService
import com.edt.ut3.backend.requests.getClient
import com.edt.ut3.misc.BaseState
import com.edt.ut3.misc.extensions.isTrue
import com.edt.ut3.misc.extensions.toList
import com.edt.ut3.misc.extensions.trigger
import com.edt.ut3.ui.preferences.formation.steps.authentication.AuthenticationFailure
import com.edt.ut3.ui.preferences.formation.steps.authentication.AuthenticationState
import com.edt.ut3.ui.preferences.formation.steps.which_groups.WhichGroupsFailure
import com.edt.ut3.ui.preferences.formation.steps.which_groups.WhichGroupsState
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

class FormationSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private var groupsDownloadJob: Job? = null

    private var client = getClient()

    private val _authenticationFailure = MutableLiveData<AuthenticationFailure?>(null)
    val authenticationFailure: LiveData<AuthenticationFailure?>
        get() = _authenticationFailure

    private val _authenticationState = MutableLiveData<AuthenticationState?>(
        AuthenticationState.Unauthenticated
    )
    val authenticationState: LiveData<AuthenticationState?>
        get() = _authenticationState

    private val _groupsFailure = MutableLiveData<WhichGroupsFailure?>(null)
    val groupsFailure: LiveData<WhichGroupsFailure?>
        get() = _groupsFailure

    private val _groupsStatus = MutableLiveData<WhichGroupsState>(null)
    val groupsStatus: LiveData<WhichGroupsState>
        get() = _groupsStatus

    private val _credentials = MutableLiveData<Credentials?>()

    private val _groups = mutableListOf<School.Info.Group>()
    val groups: List<School.Info.Group>
        get() = synchronized(_groups) { _groups }

    private val _schools: MutableList<School.Info> = application.assets
        .open("schools.json")
        .use { it.bufferedReader().readText() }
        .let(Json::decodeFromString)

    private val _selectedSchool = MutableLiveData<School.Info>(null)
    val selectedSchool: LiveData<School.Info>
        get() = _selectedSchool


    val schools: List<School.Info>
        get() = synchronized(_schools) { _schools }

    private val _groupsLD = MutableLiveData(groups)
    val groupsLD: LiveData<List<School.Info.Group>>
        get() = _groupsLD

    private val _selectedGroups = MutableLiveData<Set<School.Info.Group>>(setOf())
    val selectedGroups: LiveData<Set<School.Info.Group>>
        get() = _selectedGroups

    private val _resourceType = MutableLiveData(ResourceType.Groups)
    val resourceType get(): LiveData<ResourceType> = _resourceType

    private var authenticator: Authenticator? = null

    fun needsAuthentication(): Boolean? {
        return authenticator?.needsAuthentication
    }

    var firstCredentialsGet = true
    fun getCredentials(context: Context): LiveData<Credentials?> = synchronized(this) {
        if (firstCredentialsGet) {
            firstCredentialsGet = false
            _credentials.value = CredentialsManager.getInstance(context).getCredentials()
        }

        return _credentials
    }

    fun updateCredentials(credentials: Credentials?) {
        _credentials.value = credentials
        _authenticationState.value = AuthenticationState.Unauthenticated
    }

    suspend fun validateCredentials(): Boolean {
        val credentials = _credentials.value
        return when {
            selectedSchool.value == null || authenticator == null -> false
            credentials == null || _authenticationState.value is AuthenticationState.Authenticated || !authenticator!!.needsAuthentication -> true
            else -> ensureAuth(credentials)
        }
    }

    private suspend fun ensureAuth(credentials: Credentials): Boolean {
        _authenticationState.value = AuthenticationState.Authenticating
        return try {
            authenticator!!.ensureAuthentication(credentials)
            _authenticationState.value = AuthenticationState.Authenticated
            true
        } catch (e: Exception) {
            _authenticationFailure.value = when (e) {
                is AuthenticationException -> AuthenticationFailure.WrongCredentials
                is IOException -> AuthenticationFailure.InternetFailure
                else -> AuthenticationFailure.UnknownError
            }

            _authenticationState.value = AuthenticationState.Unauthenticated
            false
        }
    }

    fun updateGroups(context: Context) = synchronized(this) {
        groupsDownloadJob?.cancel()

        groupsDownloadJob = viewModelScope.launch {
            _groupsStatus.value = WhichGroupsState.Downloading
            val success: Boolean = try {
                val groupsLink = selectedSchool.value!!.getResource(ResourceType.Groups)
                val newGroups = CelcatService(client).getGroups(groupsLink)
                synchronized(groups) {
                    _groups.clear()
                    _groups.addAll(newGroups)
                    _groupsLD.trigger()
                }

                setupAlreadyChosenGroups(context)
                true
            } catch (e: IOException) {
                _groupsFailure.value = WhichGroupsFailure.GroupUpdateFailure
                false
            } catch (e: AuthenticationException) {
                _groupsFailure.value = WhichGroupsFailure.WrongCredentials
                false
            } catch (e: Exception) {
                e.printStackTrace()
                _groupsFailure.value = WhichGroupsFailure.UnknownError
                false
            }

            _groupsStatus.value = when (success) {
                true -> WhichGroupsState.Ready
                else -> WhichGroupsState.NotReady
            }
        }
    }

    private suspend fun setupAlreadyChosenGroups(context: Context) {
        try {
            val groupsSetInPreferences = PreferencesManager.getInstance(context).groups
            groupsSetInPreferences?.let {
                val prefGroupArray = JSONArray(groupsSetInPreferences).toList<String>()

                val prefGroups = prefGroupArray.fold(setOf<School.Info.Group>()) { acc, id ->
                    groups.find { it.id == id }?.let {
                        acc + it
                    } ?: acc
                }.toSet()

                withContext(Main) {
                    setSelectedGroups(prefGroups)
                }

            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun setSelectedGroups(groups: Set<School.Info.Group>) {
        _selectedGroups.value = groups
    }

    fun addGroup(group: School.Info.Group) {
        _selectedGroups.value = (_selectedGroups.value ?: setOf()) + group
    }

    fun removeGroup(group: School.Info.Group) {
        _selectedGroups.value = (_selectedGroups.value ?: setOf()) - group
    }

    fun validateGroups(): Boolean = _selectedGroups.value?.isNotEmpty().isTrue()

    fun setSchool(school: School.Info) {
        _selectedSchool.value = school
        authenticator = getAuthenticator(
            selectedSchool.value!!.authentication,
            client,
            selectedSchool.value!!.baseUrl
        )
        triggerAuthenticationButton()
    }

    fun validateSchool(): Boolean = _selectedSchool.value != null

    fun clearFailure(error: BaseState.Failure?) = when (error) {
        is AuthenticationFailure -> {
            _authenticationFailure.value = null
        }

        is WhichGroupsFailure -> {
            _groupsFailure.value = null
        }

        else -> {}
    }

    fun saveCredentials(context: Context) {
        if (authenticator?.needsAuthentication != true) {
            return
        }

        CredentialsManager.getInstance(context).run {
            when (val credentials = _credentials.value) {
                null -> clearCredentials()
                else -> saveCredentials(credentials)
            }
        }
    }

    fun saveSchool(context: Context) {
        PreferencesManager.getInstance(context).let { preferences ->
            preferences.school = selectedSchool.value
        }
    }

    fun saveGroups(context: Context) {
        PreferencesManager.getInstance(context).let { preferences ->
            val oldGroupsTemp = preferences.groups ?: emptyList()
            val newGroupsTemp = _selectedGroups.value?.map { it.id } ?: emptyList()

            preferences.oldGroups = oldGroupsTemp - newGroupsTemp
            preferences.groups = newGroupsTemp
            preferences.resourceType = resourceType.value ?: ResourceType.Groups
        }

        FirebaseMessagingHandler.ensureGroupRegistration(context)
    }

    fun checkConfiguration(it: Context) = PreferencesManager.getInstance(it).run {
        val configurationValid = (school != null && !groups.isNullOrEmpty())
        if (!configurationValid) {
            _authenticationFailure.value = AuthenticationFailure.ConfigurationNotFinished
        }

        configurationValid
    }

    fun triggerAuthenticationButton() = _authenticationState.trigger()

    fun selectResourceType(context: Context, resType: Int) {
        val upComingValue = ResourceType.fromUiSource(resType)
        if (_resourceType.value == upComingValue) {
            return
        }

        _resourceType.value = upComingValue
        updateGroups(context)
    }
}
