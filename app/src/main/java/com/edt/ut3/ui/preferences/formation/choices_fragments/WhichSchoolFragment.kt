package com.edt.ut3.ui.preferences.formation.choices_fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenCreated
import androidx.lifecycle.whenResumed
import com.edt.ut3.R
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.ui.preferences.formation.FormationChoiceViewModel
import kotlinx.android.synthetic.main.fragment_which_school.*
import kotlinx.coroutines.launch
import java.io.IOException

class WhichSchoolFragment: ChoiceFragment<School>() {

    private val viewModel: FormationChoiceViewModel by activityViewModels()
    private lateinit var schoolUniqueChoice: UniqueChoiceContainer<School>

    init {
        lifecycleScope.launch {
            Log.i(this@WhichSchoolFragment::class.simpleName, "Launched")
            try {
                whenCreated {
                    val formations = viewModel.getFormations()
                    Log.d(this@WhichSchoolFragment::class.simpleName, "Formations: $formations")

                    whenResumed {
                        @Suppress("UNCHECKED_CAST")
                        schoolUniqueChoice = schoolChoice as UniqueChoiceContainer<School>
                        schoolUniqueChoice.setDataSet(formations.toTypedArray()) { it.name }
                        schoolUniqueChoice.onChoiceDone = {
                            saveChoiceInViewModel()
                            onChoiceDone?.invoke()
                        }
                    }
                }
            } catch (e: IOException) {
                TODO("Display a message to enable internet")
            }
        }
    }

    override fun onCreateView (
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_which_school, container, false)

    override fun saveChoiceInViewModel() {
        println("Saving: ${schoolUniqueChoice.getChoice()}")
        viewModel.school.value = schoolUniqueChoice.getChoice()
    }
}