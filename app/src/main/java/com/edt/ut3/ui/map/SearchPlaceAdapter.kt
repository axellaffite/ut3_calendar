package com.edt.ut3.ui.map

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.edt.ut3.R
import com.edt.ut3.backend.maps.Place
import kotlinx.android.synthetic.main.search_place.view.*

class SearchPlaceAdapter(context: Context, private val values: Array<Place>) :
    ArrayAdapter<Place>(context, -1, values) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val root = if (convertView != null) {
            convertView
        } else {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            inflater.inflate(R.layout.search_place, parent, false)
        }

        val res = values[position].getIcon()

        root.icon.setImageResource(res)
        root.name.text = values[position].title

        return root
    }
}