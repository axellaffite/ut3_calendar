package com.edt.ut3.ui.preferences.formation.steps.which_school

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.edt.ut3.databinding.FragmentWhichSchoolBinding
import com.edt.ut3.databinding.LayoutSchoolBinding
import com.edt.ut3.R
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.ui.preferences.formation.FormationSelectionViewModel
import com.edt.ut3.ui.preferences.formation.steps.state_fragment.StateFragment

class FragmentWhichSchool : Fragment() {
    private val viewModel: FormationSelectionViewModel by activityViewModels()
    private lateinit var binding: FragmentWhichSchoolBinding
    private lateinit var schoolsList: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWhichSchoolBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSchools()
    }

    private fun setupSchools() {
        schoolsList = binding.schoolGroup
        binding.missingSchoolLink.setOnClickListener(::missingSchoolOnClickListener)

        for (school in viewModel.schools) {
            binding.schoolGroup.addView(addSchool(school))
        }
    }

    private fun missingSchoolOnClickListener(view: View) {
        AlertDialog
            .Builder(requireContext())
            .setMessage(R.string.school_not_in_list_dialog)
            .setPositiveButton(R.string.school_not_in_list_dialog_close) { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .also { it.show() }
            .apply { findViewById<TextView>(android.R.id.message)!!.movementMethod = LinkMovementMethod.getInstance() }
    }

    private fun addSchool(school: School.Info): View {
        val binding = LayoutSchoolBinding.inflate(layoutInflater, null, false)
        val schoolId = resources.getStringArray(R.array.school_labels).indexOf(school.label)
        val imageArray = resources.obtainTypedArray(R.array.school_icons)
        val imageRes = imageArray.getResourceId(schoolId, 0)
        if(imageRes != 0){
            val imageDrawable = ResourcesCompat.getDrawable(resources, imageRes, requireContext().theme)
            binding.schoolIcon.setImageDrawable(imageDrawable)
        }

        imageArray.recycle()

        binding.schoolLink.text = school.baseUrl
        binding.schoolName.text = resources.getStringArray(R.array.school_names)[schoolId]

        binding.root.setOnClickListener {
            viewModel.setSchool(school)
            (parentFragment as? StateFragment)?.requestNext()
        }

        return binding.root
    }
}