package com.edt.ut3.ui.preferences.formation

import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.edt.ut3.R
import com.edt.ut3.backend.background_services.BackgroundUpdater
import com.edt.ut3.databinding.FragmentWhichSchoolBinding
import com.edt.ut3.ui.preferences.formation.authentication.FragmentAuthentication
import com.edt.ut3.ui.preferences.formation.state_fragment.StateFragment
import com.edt.ut3.ui.preferences.formation.which_groups.FragmentWhichGroups
import com.edt.ut3.ui.preferences.formation.which_school.FragmentWhichSchool

class FormationSelectionFragment: StateFragment() {
    private val viewModel: FormationSelectionViewModel by activityViewModels()

    override fun initFragments(): List<StateFragmentBuilder> = listOf(
        StateFragmentBuilder(
            title = R.string.title_which_school,
            summary = R.string.summary_which_school,
            builder = { FragmentWhichSchool() },
            onNext = {
                context?.let{
                    viewModel.run {
                        saveSchool(it)
                    }
                }
                nextTo(null)
             },
            onBack = {back()},
            onRequestBack = {true},
            onRequestNext = {viewModel.validateSchool()},
            actionButtonsVisible = false
        ),
        StateFragmentBuilder(
            title = R.string.title_authentication,
            summary = R.string.summary_authentication,
            builder = { FragmentAuthentication() },
            onRequestNext = {
                context?.let {
                    viewModel.validateCredentials()
                } ?: false
            },
            onRequestBack = {
                context?.let {
                    viewModel.checkConfiguration(it)
                } ?: false
            },
            onBack = { onCancel() },
            onNext = {
                nextTo(null)
                context?.let {
                    viewModel.run {
                        saveCredentials(it)
                        updateGroups(it)
                    }
                }
            }
        ),

        StateFragmentBuilder(
            title = R.string.title_which_groups,
            summary = R.string.summary_which_groups,
            builder = {FragmentWhichGroups() },

            onRequestNext = { viewModel.validateGroups() },
            onRequestBack = { true },
            onBack = { back(); triggerAuthenticationButton() },
            onNext = { onFinish() }
        )
    )

    private fun triggerAuthenticationButton() {
        viewModel.triggerAuthenticationButton()
    }

    override fun onFinish() {
        context?.let {
            viewModel.saveGroups(it)
            BackgroundUpdater.forceUpdate(it, firstUpdate = true)
            findNavController().popBackStack()
        }
    }

    override fun onCancel() {
        findNavController().popBackStack()
    }

}