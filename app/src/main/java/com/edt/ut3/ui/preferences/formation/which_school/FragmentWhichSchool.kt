package com.edt.ut3.ui.preferences.formation.which_school

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
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
import com.edt.ut3.ui.preferences.formation.state_fragment.StateFragment

class FragmentWhichSchool : Fragment() {
    val viewModel: FormationSelectionViewModel by activityViewModels()
    private lateinit var binding: FragmentWhichSchoolBinding

    lateinit var schoolsList: LinearLayout

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
        binding.missingSchoolLink.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext()).let {
                it.setMessage(R.string.school_not_in_list_dialog)
                it.setPositiveButton(R.string.school_not_in_list_dialog_close) { dialog, _ ->
                    dialog.cancel()
                }
            }
            val dialog = builder.create()
            dialog.show()
            dialog.findViewById<TextView>(android.R.id.message)!!.movementMethod = LinkMovementMethod.getInstance()
        }

        for (school in viewModel.schools) {
            binding.schoolGroup.addView(addSchool(school))
        }
    }

    private fun addSchool(school: School.Info): View {
        val binding = LayoutSchoolBinding.inflate(layoutInflater)
        val attrs = requireContext().obtainStyledAttributes(arrayOf(R.attr.selectableItemBackground).toIntArray())

        binding.root.let{
            it.setBackgroundResource(attrs.getResourceId(0,0))
            it.isClickable = true;
            it.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        attrs.recycle()
        val schoolId = resources.getStringArray(R.array.school_labels).indexOf(school.label)
        val imageArray = resources.obtainTypedArray(R.array.school_icons)
        val imageRes = imageArray.getResourceId(schoolId, 0)
        if(imageRes != 0){
            val imageDrawable = ResourcesCompat.getDrawable(resources, imageRes, requireContext().theme)
            binding.imageView2.setImageDrawable(imageDrawable)
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