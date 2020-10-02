package com.edt.ut3.ui.preferences.formation.choices_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.edt.ut3.R

class WhichGroupFragment : ChoiceFragment<Nothing>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_which_formation, container, false)

    override fun isChoiceValid(): Boolean {
        TODO("Not yet implemented")
    }

    override fun setChoiceInViewModel(): Nothing {
        TODO("Not yet implemented")
    }


}
