package com.edt.ut3.ui.custom_views.searchbar

import androidx.core.view.doOnDetach
import kotlinx.coroutines.Job

abstract class SearchHandler {

    private var searchJob: Job? = null

    abstract fun searchLauncher(searchFunction: suspend () -> Unit): Job

    fun onSearch(job: Job) {
        searchJob?.cancel()
        searchJob = job
    }

    fun setupSearchBar(searchBar: SearchBar<*, *>) {
        searchBar.doOnDetach { searchJob?.cancel() }
    }

}