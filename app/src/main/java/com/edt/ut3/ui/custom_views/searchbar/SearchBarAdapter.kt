package com.edt.ut3.ui.custom_views.searchbar

import androidx.recyclerview.widget.RecyclerView

abstract class SearchBarAdapter<Data, ViewHolder: RecyclerView.ViewHolder> : RecyclerView.Adapter<ViewHolder>() {
    abstract fun setDataSet(dataSet: List<Data>)
}