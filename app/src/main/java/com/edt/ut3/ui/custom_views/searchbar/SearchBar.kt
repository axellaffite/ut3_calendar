package com.edt.ut3.ui.custom_views.searchbar

import android.content.Context
import android.util.AttributeSet
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.edt.ut3.R
import kotlinx.android.synthetic.main.layout_search_bar.view.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import java.util.*

class SearchBar<Data, Adapter: SearchBarAdapter<Data, *>> (context: Context, attrs: AttributeSet? = null)
    : CardView(context, attrs)
{

    init {
        inflate(context, R.layout.layout_search_bar, this)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SearchBar,
            0, 0).apply {

            try {
                setCardBackgroundColor(getColor(R.styleable.SearchBar_backgroundColor, ContextCompat.getColor(context, R.color.backgroundColor)))
                search_bar.hint = getString(R.styleable.SearchBar_placeHolder) ?: "..."
            } finally {
                recycle()
            }
        }

        search_bar.doOnTextChanged { text, _, _, _ ->
            if (hasFocus()) {
                search()
            }
        }

    }

    private var mConverter: ((Data) -> String)? = null
    private var mDataSet: List<Data>? = null
    private var mSearchHandler: SearchHandler? = null

    private var filteredDataSet = mutableListOf<Data>()

    fun configure(dataSet: List<Data>,
                  converter: (Data) -> String,
                  searchHandler: SearchHandler,
                  adapter: Adapter)
    {
        this.mDataSet = dataSet
        this.mConverter = converter
        this.mSearchHandler = searchHandler
        this.setAdapter(adapter)
    }

    fun removeFilters(vararg chips: FilterChip<Data>) {
        chips.forEach {
            filters?.removeView(it)
        }
    }

    fun clearFilters() {
        filters?.removeAllViews()
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

    @Suppress("UNCHECKED_CAST")
    fun getFilters(): List<FilterChip.Filter<Data>> = filters?.children?.map {
        with (it as? FilterChip<Data>) {
            if (this?.isChecked == true) {
                this.filter
            } else {
                null
            }
        }
    }?.filterNotNull()?.toList() ?: listOf()

    private suspend fun search(word: String, query: String): Boolean = withContext(Default) {
        var i = 0
        var j = 0

        while (i < word.length && j < query.length) {
            while (i < word.length) {
                if (word[i] == query[j]) {
                    j += 1
                    break
                }

                i += 1
            }
            i += 1
        }

        j == query.length
    }

    fun search(matchSearchBarText: Boolean = true, callback: ((List<Data>) -> Unit)? = null) {
        val query = if (matchSearchBarText) {
            search_bar?.text.toString()
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
                search(dataConverter(it).uppercase(Locale.FRENCH), upperText)
            }.let {
                localFilters.fold(it) { acc, filter -> filter(acc) }
            }.let {
                globalFilters(it)
            }
        }


        filteredDataSet.clear()
        filteredDataSet.addAll(filtered)

        withContext(Main) {
            if (query == search_bar?.text.toString()) {
                val res = results?.adapter
                res?.notifyDataSetChanged()
            }

            callback?.invoke(filtered)
        }

    }

}