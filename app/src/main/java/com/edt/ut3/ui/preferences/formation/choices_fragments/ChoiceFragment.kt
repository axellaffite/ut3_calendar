package com.edt.ut3.ui.preferences.formation.choices_fragments

import androidx.fragment.app.Fragment

abstract class ChoiceFragment<Choice>: Fragment() {

    abstract fun isChoiceValid(): Boolean

    @Throws(IllegalStateException::class)
    abstract fun setChoiceInViewModel(): Choice

}