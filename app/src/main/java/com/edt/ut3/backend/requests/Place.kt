package com.edt.ut3.backend.requests

import androidx.room.Entity
import androidx.room.TypeConverters
import com.edt.ut3.R
import com.edt.ut3.backend.database.Converter
import com.edt.ut3.misc.map
import com.edt.ut3.misc.realOpt
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.util.*

@Entity(
    tableName = "place_info",
    primaryKeys = ["title", "type"]
)
data class Place (
    var id: String?,
    var title: String,
    var short_desc: String?,
    @TypeConverters(Converter::class) var geolocalisation: GeoPoint,
    var type: String,
    var photo: String?,
    var contact: String?
) {
    companion object {
        @Throws(JSONException::class)
        fun fromJSON(json: JSONObject): Place {
            val fields = json["fields"] as JSONObject
            val localisation = (fields["geolocalisation"] as JSONArray).map { it as Double }
            return Place (
                id = fields.optString("id"),
                title = fields.getString("title"),
                short_desc = fields.realOpt("short_desc"),
                geolocalisation = GeoPoint(localisation[0], localisation[1]),
                type = fields.getString("type"),
                photo = fields.realOpt("photo"),
                contact = fields.realOpt("contact")
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