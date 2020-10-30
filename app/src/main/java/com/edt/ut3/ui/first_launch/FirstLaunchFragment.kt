package com.edt.ut3.ui.first_launch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.edt.ut3.R
import com.edt.ut3.misc.extensions.addOnBackPressedListener
import kotlinx.android.synthetic.main.fragment_first_launch.*

class FirstLaunchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_first_launch, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lets_go.setOnClickListener { findNavController().navigate(
            R.id.action_firstLaunchFragment_to_formationSelectionFragment
        ) }

        addOnBackPressedListener {
            activity?.finish()
        }
    }

}