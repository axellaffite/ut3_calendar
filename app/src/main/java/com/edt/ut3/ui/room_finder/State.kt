package com.edt.ut3.ui.room_finder

import android.content.Context
import com.edt.ut3.R
import com.edt.ut3.misc.BaseState

sealed class RoomFinderState: BaseState.FeatureState() {
    object Presentation: RoomFinderState()
    object Downloading: RoomFinderState()
    object Searching: RoomFinderState()
    object Result: RoomFinderState()
}

abstract class RoomFinderFailure(private val reason: Int): BaseState.Failure() {

    override fun reason(context: Context) = context.getString(reason)

    object InternetFailure: RoomFinderFailure(R.string.error_check_internet)
    object SearchFailure: RoomFinderFailure(R.string.error_check_internet)
    object UpdateBuildingFailure: RoomFinderFailure(R.string.error_check_internet)

}