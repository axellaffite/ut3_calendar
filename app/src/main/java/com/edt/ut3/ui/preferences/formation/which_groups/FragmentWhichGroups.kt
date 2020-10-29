package com.edt.ut3.ui.preferences.formation.which_groups

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.edt.ut3.R
import com.edt.ut3.backend.formation_choice.School
import com.edt.ut3.misc.extensions.discard
import com.edt.ut3.ui.custom_views.searchbar.SearchBar
import com.edt.ut3.ui.custom_views.searchbar.SearchBarAdapter
import com.edt.ut3.ui.custom_views.searchbar.SearchHandler
import com.edt.ut3.ui.preferences.formation.FormationSelectionViewModel
import com.edt.ut3.ui.preferences.formation.StepperElement
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_which_groups.*
import kotlinx.android.synthetic.main.layout_search_bar.view.*
import kotlinx.coroutines.Job

class FragmentWhichGroups: StepperElement() {

    val viewModel: FormationSelectionViewModel by activityViewModels()
    lateinit var searchBar : SearchBar<School.Info.Group, GroupAdapter>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_which_groups, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearchBar()
        setupListeners()
    }

    private fun setupSearchBar() {
        @Suppress("UNCHECKED_CAST")
        searchBar = group_search_bar as SearchBar<School.Info.Group, GroupAdapter>
        searchBar.configure(
            dataSet = viewModel.groups,
            converter = { it.text },
            searchHandler = GroupSearchHandler(),
            adapter = GroupAdapter().apply {
                onItemClicked = { _: View, _: Int, group: School.Info.Group ->
                    viewModel.addGroup(group)
                    searchBar.clearFocus()
                }
            }
        )
    }

    private fun setupListeners() {
        viewModel.run {
            groupsLD.observe(viewLifecycleOwner) {
                searchBar.results?.adapter?.notifyDataSetChanged()
                searchBar.results?.visibility = VISIBLE
            }

            selectedGroups.observe(viewLifecycleOwner) { selectedGroups: Set<School.Info.Group> ->
                updateChips(selectedGroups)
            }

            groupsStatus.observe(viewLifecycleOwner, ::handleStateChange)
            groupsFailure.observe(viewLifecycleOwner, ::handleError)
        }

        searchBar.search_bar.setOnFocusChangeListener { v, hasFocus ->
            Log.d("Which groups", "Search bar has focus: $hasFocus")
            if (hasFocus) {
                searchBar.search()
                searchBar.results.visibility = VISIBLE
            } else {
                searchBar.results.visibility = GONE
            }
        }
    }

    private fun handleStateChange(state: WhichGroupsState?) : Unit = when (state) {
        WhichGroupsState.NotReady -> {
            searchBar.search_bar?.isEnabled = false
            selectedGroups?.visibility = VISIBLE
            loading?.visibility = GONE
            errorMessage?.visibility = GONE
        }

        WhichGroupsState.Downloading -> {
            searchBar.search_bar?.isEnabled = false
            selectedGroups?.visibility = GONE
            loading?.visibility = VISIBLE
            errorMessage?.visibility = GONE
        }

        WhichGroupsState.Ready -> {
            searchBar.search_bar?.isEnabled = true
            selectedGroups?.visibility = VISIBLE
            loading?.visibility = GONE
            errorMessage?.visibility = GONE
        }

        else -> {}
    }.also { Log.d("FragmentWhichGroups", "State set to $state") }

    private fun handleError(error: WhichGroupsFailure?) : Unit = when (error) {
        WhichGroupsFailure.WrongCredentials, WhichGroupsFailure.UnknownError -> {
            snack_container?.let {
                Snackbar.make(it, error.reason(it.context), Snackbar.LENGTH_SHORT).show()
            }.discard()
        }

        WhichGroupsFailure.GroupUpdateFailure -> {
            snack_container?.let {
                Snackbar.make(it, error.reason(it.context), Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.action_retry) { v ->
                        viewModel.updateGroups(v.context)
                    }
                    .show()
            }.discard()
        }

        else -> {}
    }.also { viewModel.clearFailure(error) }

    private fun updateChips(groups: Set<School.Info.Group>) {
        selectedGroups?.removeAllViews()
        groups.forEach { group ->
            selectedGroups?.addView(Chip(requireContext()).apply {
                text = if (group.text.length > 20) {
                    group.text.substring(0, 10) + "..." + group.text.substring(group.text.length - 10)
                } else {
                    group.text
                }

                isCheckable = false

                setOnCloseIconClickListener {
                    viewModel.removeGroup(group)
                }
            })
        }
    }


    class GroupAdapter : SearchBarAdapter<School.Info.Group, GroupViewHolder>() {
        lateinit var mDataSet : List<School.Info.Group>

        var onItemClicked : ((item: View, position: Int, group: School.Info.Group) -> Unit)? = null

        override fun setDataSet(dataSet: List<School.Info.Group>) {
            mDataSet = dataSet
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.layout_group, parent, false) as TextView
            v.setTextColor(ContextCompat.getColor(parent.context, R.color.textColor))

            return GroupViewHolder(v)
        }

        override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
            val item = mDataSet[position]
            holder.item.apply {
                text = mDataSet[position].text

                setOnClickListener {
                    onItemClicked?.invoke(it, position, item)
                }
            }
        }

        override fun getItemCount() = mDataSet.size

    }

    class GroupViewHolder(val item: TextView) : RecyclerView.ViewHolder(item)

    inner class GroupSearchHandler: SearchHandler() {
        override fun searchLauncher(searchFunction: suspend () -> Unit): Job {
            return lifecycleScope.launchWhenResumed { searchFunction() }
        }
    }

}