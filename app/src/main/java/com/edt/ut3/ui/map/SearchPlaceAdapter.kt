package com.edt.ut3.ui.map

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.edt.ut3.R
import com.edt.ut3.misc.map
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.search_place.view.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.util.*

class SearchPlaceAdapter(context: Context, private val values: Array<Place>) :
    ArrayAdapter<SearchPlaceAdapter.Place>(context, -1, values) {

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

    data class Place (
        val id: String?,
        val title: String,
        val short_desc: String?,
        val geolocalisation: GeoPoint,
        val type: String,
        val photo: String?,
        val contact: String?
    ) {
        companion object {
            @Throws(JSONException::class)
            fun fromJSON(json: JSONObject): Place {
                val fields = json["fields"] as JSONObject
                val localisation = (fields["geolocalisation"] as JSONArray).map { it as Double }
                return Place (
                    id = fields.optString("id"),
                    title = fields.getString("title"),
                    short_desc = fields.optString("short_desc"),
                    geolocalisation = GeoPoint(localisation[0], localisation[1]),
                    type = fields.getString("type"),
                    photo = fields.optString("photo"),
                    contact = fields.optString("contact")
                )
            }
        }

        fun getIcon() = when (type.toLowerCase(Locale.getDefault())) {
            "batiment" -> R.drawable.ic_building
            "épicerie" -> R.drawable.ic_grocery
            "foodtruck" -> R.drawable.ic_foodtruck
            "triporteur" -> R.drawable.ic_foodtruck
            else -> R.drawable.ic_restaurant
        }
    }
}