package com.edt.ut3.ui.map

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.edt.ut3.R
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.search_place.view.*

class SearchPlaceAdapter(context: Context, private val values: Array<Place>) :
    ArrayAdapter<SearchPlaceAdapter.Place>(context, -1, values) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val root = inflater.inflate(R.layout.search_place, parent, false)

        val res = when (values[position].category) {
            "caféteria", "restaurant" -> R.drawable.ic_restaurant
             "batiment" -> R.drawable.ic_building
            "amphithéatre" -> R.drawable.ic_amphitheater
            else -> R.drawable.ic_restaurant
        }

        root.icon.setImageResource(res)
        root.name.text = values[position].value

        return root
    }

    data class Place (
        val category: String,
        val value: String,
        val location: LatLng
    )
}