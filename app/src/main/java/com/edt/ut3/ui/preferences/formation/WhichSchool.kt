package com.edt.ut3.ui.preferences.formation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.view.get
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.edt.ut3.R
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.misc.Optional
import com.edt.ut3.misc.extensions.getSelectedItemIndex
import kotlinx.android.synthetic.main.fragment_which_school.*
import java.io.IOException

class WhichSchool: StepperElement() {

    val viewModel: FormationViewModel by activityViewModels()

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selected", school_group?.getSelectedItemIndex() ?: -1)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_which_school, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        downloadSchools {
            savedInstanceState?.run {
                val selected = getInt("selected")
                if (selected in 0 until school_group.childCount) {
                    (school_group?.get(selected) as? RadioButton)?.let { it.isChecked = true }
                }
            }
        }
    }

    private fun downloadSchools(callback: () -> Unit) {
        loading.visibility = VISIBLE
        lifecycleScope.launchWhenResumed {
            try {
                val schools = viewModel.getSchools()
                setupSchools(schools)
                callback()
            } catch (e: IOException) {
                showInternetErrorMessage(callback)
            } finally {
                loading.visibility = INVISIBLE
            }
        }
    }

    private fun setupSchools(schools: List<School>) {
        (schools + null).forEach { school ->
            school_group.addView(RadioButton(requireContext()).apply {
                text = school?.name ?: getString(R.string.not_listed)

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        viewModel.school.value = Optional.of(school)
                    }
                }
            })
        }
    }

    private fun showInternetErrorMessage(callback: () -> Unit) {
        error.visibility = VISIBLE
        retry.setOnClickListener {
            error.visibility = INVISIBLE
            downloadSchools(callback)
        }
    }

}