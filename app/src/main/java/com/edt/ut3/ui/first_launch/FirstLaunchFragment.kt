package com.edt.ut3.ui.first_launch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.edt.ut3.R
import com.edt.ut3.databinding.FragmentFirstLaunchBinding
import com.edt.ut3.misc.extensions.addOnBackPressedListener

class FirstLaunchFragment : Fragment() {

    private lateinit var binding: FragmentFirstLaunchBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFirstLaunchBinding.inflate(inflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.letsGo.setOnClickListener { findNavController().navigate(
            R.id.action_firstLaunchFragment_to_formationSelectionFragment
        ) }

        addOnBackPressedListener {
            activity?.finish()
        }
    }

}