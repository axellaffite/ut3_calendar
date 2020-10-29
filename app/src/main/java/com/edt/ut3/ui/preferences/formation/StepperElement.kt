package com.edt.ut3.ui.preferences.formation

import androidx.fragment.app.Fragment
import com.edt.ut3.R
import com.edt.ut3.ui.preferences.formation.authentication.FragmentAuthentication
import com.edt.ut3.ui.preferences.formation.which_groups.FragmentWhichGroups

abstract class StepperElement: Fragment() {

    companion object {
        fun<T: StepperElement> titleOf(c: Class<T>) = when (c) {

            FragmentWhichGroups::class.java -> R.string.title_which_groups
            FragmentAuthentication::class.java -> R.string.title_authentication
            else -> throw IllegalStateException("titleOf() not implemented for $c")
        }

        fun<T: StepperElement> summaryOf(c: Class<T>) = when (c) {
            FragmentAuthentication::class.java -> R.string.summary_authentication
            FragmentWhichGroups::class.java -> R.string.summary_which_groups
            else -> throw IllegalStateException("summaryOf() not implemented for $c")
        }
    }

}