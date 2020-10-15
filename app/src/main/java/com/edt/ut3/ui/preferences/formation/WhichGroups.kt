package com.edt.ut3.ui.preferences.formation

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
import com.edt.ut3.backend.preferences.PreferencesManager
import com.edt.ut3.backend.requests.CelcatService
import com.edt.ut3.misc.extensions.toList
import com.edt.ut3.ui.custom_views.searchbar.SearchBar
import com.edt.ut3.ui.custom_views.searchbar.SearchBarAdapter
import com.edt.ut3.ui.custom_views.searchbar.SearchHandler
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_which_groups.*
import kotlinx.android.synthetic.main.layout_search_bar.view.*
import kotlinx.coroutines.Job
import org.json.JSONArray
import org.json.JSONException

class WhichGroups: StepperElement() {

    val viewModel: FormationViewModel by activityViewModels()
    lateinit var searchBar : SearchBar<School.Info.Group, GroupAdapter>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_which_groups, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        @Suppress("UNCHECKED_CAST")
        searchBar = group_search_bar as SearchBar<School.Info.Group, GroupAdapter>
        searchBar.configure(
            dataSet = viewModel.availableGroups,
            converter = { it.text },
            searchHandler = GroupSearchHandler(),
            adapter = GroupAdapter().apply {
                onItemClicked = { view: View, position: Int, group: School.Info.Group ->
                    viewModel.groups.value = (viewModel.groups.value ?: setOf()) + group
                    searchBar.clearFocus()
                }
            }
        )

        viewModel.link.observe(viewLifecycleOwner) { opt ->
            opt.ifInit { nullableInfo ->
                nullableInfo?.let { info ->
                    setupSearchBar(info)
                }
            }
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

        viewModel.groups.observe(viewLifecycleOwner) { groups: Set<School.Info.Group> ->
            updateChips(groups)
       }

    }

    private fun setupSearchBar(link: School.Info) {
        lifecycleScope.launchWhenResumed {
            try {
                val groups = CelcatService().getGroups(link.groups)
                viewModel.availableGroups.apply {
                    clear()
                    addAll(groups)
                }

                viewModel.groups.apply {
                    if (value.isNullOrEmpty()) {
                        try {
                            val groupsSetInPreferences = PreferencesManager.getInstance(requireContext()).groups
                            groupsSetInPreferences?.let {
                                val prefGroupArray = JSONArray(groupsSetInPreferences).toList<String>()

                                val prefGroups = prefGroupArray.fold(setOf<School.Info.Group>()) { acc, id ->
                                    groups.find { it.id == id }?.let {
                                        acc + it
                                    } ?: acc
                                }.toSet()

                                value = prefGroups
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                }

                searchBar.results?.adapter?.notifyDataSetChanged()
                searchBar.results?.visibility = VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
                snack_container?.let {
                    Snackbar.make(it, R.string.error_check_internet, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.action_retry) {
                            setupSearchBar(link)
                        }
                        .show()
                }
            }
        }
    }

    private fun updateChips(groups: Set<School.Info.Group>) {
        chipGroup?.removeAllViews()
        groups.forEach { group ->
            chipGroup?.addView(Chip(requireContext()).apply {
                text = if (group.text.length > 20) {
                    group.text.substring(0, 10) + "..." + group.text.substring(group.text.length - 10)
                } else {
                    group.text
                }

                isCheckable = false

                setOnCloseIconClickListener {
                    viewModel.groups.value = (viewModel.groups.value ?: setOf()) - group
                    chipGroup?.removeView(it)
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