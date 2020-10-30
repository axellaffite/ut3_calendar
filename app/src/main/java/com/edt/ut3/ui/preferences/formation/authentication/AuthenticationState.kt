package com.edt.ut3.ui.preferences.formation.authentication

import android.content.Context
import com.edt.ut3.R
import com.edt.ut3.misc.BaseState

abstract class AuthenticationState: BaseState.FeatureState() {
    object Unauthenticated: AuthenticationState()
    object Authenticating: AuthenticationState()
    object Authenticated: AuthenticationState()
}

abstract class AuthenticationFailure(private val reason: Int): BaseState.Failure() {
    override fun reason(context: Context) = context.getString(reason)

    object InternetFailure: AuthenticationFailure(R.string.error_check_internet)
    object WrongCredentials: AuthenticationFailure(R.string.error_wrong_credentials)
    object UnknownError: AuthenticationFailure(R.string.unknown_error)
    object ConfigurationNotFinished: AuthenticationFailure(R.string.error_complete_configuration)
}