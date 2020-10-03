package com.edt.ut3.ui.preferences.formation.choices_fragments

import androidx.fragment.app.Fragment

abstract class ChoiceFragment<Choice>: Fragment() {

    var onChoiceDone: (() -> Unit)? = null

    abstract fun saveChoiceInViewModel()

}