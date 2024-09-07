package com.edt.ut3.ui.preferences.formation.steps.which_groups

import android.content.Context
import com.edt.ut3.R
import com.edt.ut3.misc.BaseState

abstract class WhichGroupsState: BaseState.FeatureState() {
    object NotReady: WhichGroupsState()
    object Downloading: WhichGroupsState()
    object Ready: WhichGroupsState()
}

abstract class WhichGroupsFailure(private val reason: Int) : BaseState.Failure() {
    override fun reason(context: Context) = context.getString(reason)

    object GroupUpdateFailure: WhichGroupsFailure(R.string.error_check_internet)
    object WrongCredentials : WhichGroupsFailure(R.string.error_wrong_credentials)
    object UnknownError : WhichGroupsFailure(R.string.unknown_error)
}