package com.edt.ut3.ui.custom_views.searchbar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edt.ut3.R
import com.edt.ut3.databinding.LayoutSearchBarBinding
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import java.util.*

class SearchBar<Data, Adapter: SearchBarAdapter<Data, *>> (context: Context, attrs: AttributeSet? = null)
    : CardView(context, attrs)
{
    private var binding: LayoutSearchBarBinding =
        LayoutSearchBarBinding.inflate(LayoutInflater.from(context), this)

    // parce qu'il y a du code ailleurs qui en a besoin...
    val searchBar: EditText = binding.searchBar
    var results: RecyclerView = binding.results
    private val filters = binding.filters
    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SearchBar,
            0, 0).apply {

            try {
                setCardBackgroundColor(getColor(R.styleable.SearchBar_backgroundColor, ContextCompat.getColor(context, R.color.backgroundColor)))
                searchBar.hint = getString(R.styleable.SearchBar_placeHolder) ?: "..."
            } finally {
                recycle()
            }
        }

        searchBar.doOnTextChanged { text, _, _, _ ->
            if (hasFocus()) {
                search()
            }
        }

    }

    private var mConverter: ((Data) -> String)? = null
    private var mDataSet: List<Data>? = null
    private var mSearchHandler: SearchHandler? = null

    private var filteredDataSet = mutableListOf<Data>()

    fun hideResults() {
        results.visibility = View.GONE
    }

    fun showResults() {
        results.visibility = View.VISIBLE
    }

    fun hideFilters() {
        results.visibility = View.GONE
    }

    fun showFilters() {
        results.visibility = View.VISIBLE
    }

    fun configure(
        dataSet: List<Data>,
        converter: (Data) -> String,
        searchHandler: SearchHandler,
        adapter: Adapter
    ) {
        this.mDataSet = dataSet
        this.mConverter = converter
        this.mSearchHandler = searchHandler
        this.setAdapter(adapter)
    }

    fun removeFilters(vararg chips: FilterChip<Data>) {
        chips.forEach {
            filters.removeView(it)
        }
    }

    fun clearFilters() {
        filters.removeAllViews()
    }

    fun setFilters(vararg chips: FilterChip<Data>) {
        clearFilters()
        addFilters(*chips)
    }

    fun addFilters(vararg chips: FilterChip<Data>) {
        chips.forEach {
            filters.addView(it.apply {
                setOnCheckedChangeListener { _, _ ->
                    search()
                }
            })
        }
    }

    private fun setAdapter(adapter: Adapter) {
        results.layoutManager = LinearLayoutManager(context)
        results.adapter = adapter
        adapter.setDataSet(filteredDataSet)
    }

    fun setDataSet(dataSet: List<Data>) {
        mDataSet = dataSet
        search(matchSearchBarText = true)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getFilters(): List<FilterChip.Filter<Data>> = filters?.children?.mapNotNull {
        with (it as? FilterChip<Data>) {
            this?.filter?.takeIf { this.isChecked }
        }
    }?.toList() ?: emptyList()

    fun search(matchSearchBarText: Boolean = true, callback: ((List<Data>) -> Unit)? = null) {
        val query = if (matchSearchBarText) {
            searchBar.text.toString()
        } else { String() }


        mSearchHandler?.searchLauncher {
            search(query, callback)
        }
    }

    private suspend fun search(query: String, callback: ((List<Data>) -> Unit)?) {
        val dataSetToFilter = mDataSet ?: return
        val dataConverter = mConverter ?: return
        val filters = getFilters()
        val globalFilters =
            filters
                .filterIsInstance<FilterChip.GlobalFilter<Data>>()
                .fold(FilterChip.GlobalFilter<Data>(null)) { acc, filter -> acc + filter }

        val localFilters = filters.filterIsInstance<FilterChip.LocalFilter<Data>>()
        val upperText = query.uppercase(Locale.FRENCH)


        val filtered = withContext(Default) {
            dataSetToFilter.filter {
                dataConverter(it).contains(upperText, ignoreCase = true)
            }.let {
                localFilters.fold(it) { acc, filter -> filter(acc) }
            }.let {
                globalFilters(it)
            }.sortedBy { dataConverter(it).indexOf(upperText, ignoreCase = true) }
        }


        filteredDataSet.clear()
        filteredDataSet.addAll(filtered)

        withContext(Main) {
            if (query == searchBar.text.toString()) {
                val res = results.adapter
                res?.notifyDataSetChanged()
            }

            callback?.invoke(filtered)
        }

    }

}