package com.edt.ut3.ui.map

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.edt.ut3.R
import com.edt.ut3.backend.maps.Place
import com.edt.ut3.databinding.SearchPlaceBinding

class SearchPlaceAdapter(context: Context, private val values: Array<Place>) :
    ArrayAdapter<Place>(context, -1, values) {
    private var binding: SearchPlaceBinding? = null
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val root = if (convertView != null) {
            convertView
        } else {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            //inflater.inflate(R.layout.search_place, parent, false)
            binding = SearchPlaceBinding.inflate(inflater)
        }

        val res = values[position].getIcon()

        binding!!.icon.setImageResource(res)
        binding!!.name.text = values[position].title

        return binding!!.root
    }
}