package com.edt.ut3.ui.preferences.formation.choices_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.edt.ut3.R
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.ui.preferences.formation.FormationChoiceViewModel
import kotlinx.android.synthetic.main.fragment_which_link.*

class WhichLinkFragment : ChoiceFragment<Nothing>() {

    val viewModel: FormationChoiceViewModel by activityViewModels()
    lateinit var linkChoice: UniqueChoiceContainer<School.Info>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_which_link, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        @Suppress("UNCHECKED_CAST")
        linkChoice = link_choice as UniqueChoiceContainer<School.Info>

        viewModel.school.observe(viewLifecycleOwner) { school ->
            linkChoice.setDataSet(school.info.toTypedArray()) { info ->
                info.url
            }
        }

        println("Value=${viewModel.school.value}")
        viewModel.school.value?.let { school ->
            linkChoice.setDataSet(school.info.toTypedArray()) {
                println(it)
                it.url
            }
        }
    }

    override fun saveChoiceInViewModel() {
        viewModel.link.value = linkChoice.getChoice()
    }

}
