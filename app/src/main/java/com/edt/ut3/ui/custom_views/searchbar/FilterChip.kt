package com.edt.ut3.ui.custom_views.searchbar

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.edt.ut3.R
import com.edt.ut3.misc.extensions.isNotNull
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable

open class FilterChip<Data>(context: Context, attrs: AttributeSet? = null): Chip(context, attrs) {

    var filter: Filter<Data>? = null

    init {
        setChipDrawable(
            ChipDrawable.createFromAttributes(
                context,
                null,
                0,
                R.style.Widget_MaterialComponents_Chip_Filter
            )
        )

        setChipBackgroundColorResource(R.color.foregroundColor)
        setTextColor(ContextCompat.getColor(context, R.color.textColor))
    }

    abstract class Filter<Data>() {
        abstract operator fun invoke(dataSet: List<Data>): List<Data>
    }

    /**
     * This filter is used to filter a value.
     * The resulting list relies on this filter and
     * all of the GlobalFilters that have been declared
     * in the ChipGroup.
     *
     * Example :
     *  - 1st global filter : value must be equal to "hello"
     *  - 2nd global filter : value must be equal to "world"
     *
     * Take apart, the first filter will only keep the values that
     * are equals to "hello".
     * Same behavior for the second one but with the value "world".
     *
     * Il we combine them all, we have a new filter that will
     * keep the words "hello" AND "world".
     */
    class GlobalFilter<Data>(filter: ((Data) -> Boolean)?): Filter<Data>() {

        private val filters = mutableListOf<(Data) -> Boolean>()

        init {
            filter?.let {
                filters.add(filter)
            }
        }

        operator fun plus(filter: GlobalFilter<Data>): GlobalFilter<Data> {
            val totalFilters = filters + filter.filters

            return GlobalFilter<Data>(null).apply {
                filters += totalFilters
            }
        }

        override fun invoke(dataSet: List<Data>): List<Data> {
            return if (filters.isEmpty()) {
                dataSet
            } else {
                dataSet.filter {
                    filters.firstOrNull { f -> f(it) }.isNotNull()
                }
            }
        }
    }

    /**
     * This filter is used to filter a list.
     * The resulting list relies only on itself
     * no matter what the other filters.
     *
     * @param Data
     * @property filter
     */
    class LocalFilter<Data>(val filter : ((List<Data>) -> List<Data>)): Filter<Data>() {
        override fun invoke(dataSet: List<Data>) = filter(dataSet)
    }

}