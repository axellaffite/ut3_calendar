package com.edt.ut3.ui.preferences.formation

import androidx.fragment.app.Fragment
import com.edt.ut3.R

abstract class StepperElement: Fragment() {

    companion object {
        fun<T: StepperElement> titleOf(c: Class<T>) = when (c) {

            WhichGroups::class.java -> R.string.title_which_groups
            FormationAuthentication::class.java -> R.string.title_authentication
            else -> throw IllegalStateException("titleOf() not implemented for $c")
        }

        fun<T: StepperElement> summaryOf(c: Class<T>) = when (c) {
            FormationAuthentication::class.java -> R.string.summary_authentication
            WhichGroups::class.java -> R.string.summary_which_groups
            else -> throw IllegalStateException("summaryOf() not implemented for $c")
        }
    }

}