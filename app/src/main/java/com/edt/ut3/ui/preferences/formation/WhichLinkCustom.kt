package com.edt.ut3.ui.preferences.formation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import com.edt.ut3.R
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.misc.Optional
import kotlinx.android.synthetic.main.fragment_which_link_custom.*

class WhichLinkCustom: StepperElement() {

    val viewModel: FormationViewModel by activityViewModels()

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("input", input.text.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_which_link_custom, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.run {
            input.setText(getString("input"))
        }

        input.doOnTextChanged { text, _, _, _ ->
            try {
                val infoGroups = School.Info.fromClassicLink(text.toString())
                val info = infoGroups.first
                val groups = infoGroups.second

                viewModel.link.value = Optional.of(info)
                viewModel.groups.value = groups.map { School.Info.Group(it, it) }.toSet()
            } catch (e: School.Info.InvalidLinkException) {
                input.error = getString(e.reason)
            }
        }
    }

}