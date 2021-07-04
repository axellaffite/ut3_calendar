package com.edt.ut3.ui.preferences.formation

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edt.ut3.backend.credentials.CredentialsManager
import com.edt.ut3.backend.firebase_services.FirebaseMessagingHandler
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.backend.requests.celcat.CelcatService
import com.edt.ut3.backend.network.getClient
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.backend.requests.authentication_services.AuthenticationException
import com.edt.ut3.backend.requests.authentication_services.AuthenticatorUT3
import com.edt.ut3.backend.requests.authentication_services.Credentials
import com.edt.ut3.misc.BaseState
import com.edt.ut3.misc.extensions.isTrue
import com.edt.ut3.misc.extensions.toList
import com.edt.ut3.misc.extensions.trigger
import com.edt.ut3.ui.preferences.formation.authentication.AuthenticationFailure
import com.edt.ut3.ui.preferences.formation.authentication.AuthenticationState
import com.edt.ut3.ui.preferences.formation.which_groups.WhichGroupsFailure
import com.edt.ut3.ui.preferences.formation.which_groups.WhichGroupsState
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

class FormationSelectionViewModel: ViewModel() {
    private var groupsDownloadJob: Job? = null

    private val _authenticationFailure = MutableLiveData<AuthenticationFailure?>(null)
    val authenticationFailure : LiveData<AuthenticationFailure?>
        get() = _authenticationFailure

    private val _authenticationState = MutableLiveData<AuthenticationState?>(
        AuthenticationState.Unauthenticated
    )
    val authenticationState : LiveData<AuthenticationState?>
        get() = _authenticationState

    private val _groupsFailure = MutableLiveData<WhichGroupsFailure?>(null)
    val groupsFailure : LiveData<WhichGroupsFailure?>
        get() = _groupsFailure

    private val _groupsStatus = MutableLiveData<WhichGroupsState>(null)
    val groupsStatus : LiveData<WhichGroupsState>
        get() = _groupsStatus

    private val _credentials = MutableLiveData<Credentials?>()

    private val _groups = mutableListOf<School.Info.Group>()
    val groups : List<School.Info.Group>
        get() = synchronized(_groups) {_groups}

    private val _groupsLD = MutableLiveData(groups)
    val groupsLD : LiveData<List<School.Info.Group>>
        get() = _groupsLD

    private val _selectedGroups = MutableLiveData<Set<School.Info.Group>>(setOf())
    val selectedGroups : LiveData<Set<School.Info.Group>>
        get() = _selectedGroups

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

    suspend fun validateCredentials(context: Context): Boolean {
        val credentials = _credentials.value
        return if (credentials == null || _authenticationState.value is AuthenticationState.Authenticated) {
            true
        } else {
            _authenticationState.value = AuthenticationState.Authenticating
            try {
                AuthenticatorUT3(getClient()).checkCredentials(credentials)
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
    }

    fun updateGroups(context: Context) = synchronized(this) {
        if (groupsDownloadJob?.isActive.isTrue() || groups.isNotEmpty()) {
            return
        }

        groupsDownloadJob = viewModelScope.launch {
            _groupsStatus.value = WhichGroupsState.Downloading
            val success: Boolean = try {
                val newGroups = CelcatService(getClient()).getGroups(School.default.info.first().groups)
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

    fun validateGroups() : Boolean = _selectedGroups.value?.isNotEmpty().isTrue()

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
        CredentialsManager.getInstance(context).run {
            when (val credentials = _credentials.value) {
                null -> clearCredentials()
                else -> saveCredentials(credentials)
            }
        }
    }

    fun saveGroups(context: Context) {
        PreferencesManager.getInstance(context).apply {
            val oldGroupsTemp = this.groups ?: listOf()
            val newGroupsTemp = _selectedGroups.value?.map { it.id } ?: listOf()

            oldGroups = oldGroupsTemp - newGroupsTemp
            groups = newGroupsTemp
            link = School.default.info.first()
        }

        FirebaseMessagingHandler.ensureGroupRegistration(context)
    }

    fun checkConfiguration(it: Context) = PreferencesManager.getInstance(it).run {
        val configurationValid = (link != null && !groups.isNullOrEmpty())
        if (!configurationValid) {
            _authenticationFailure.value = AuthenticationFailure.ConfigurationNotFinished
        }

        configurationValid
    }

    fun triggerAuthenticationButton() = _authenticationState.trigger()
}
