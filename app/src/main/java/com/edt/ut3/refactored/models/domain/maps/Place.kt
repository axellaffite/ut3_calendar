package com.edt.ut3.refactored.models.domain.maps

import androidx.room.Entity
import androidx.room.TypeConverters
import com.edt.ut3.R
import com.edt.ut3.refactored.models.repositories.database.Converter
import kotlinx.serialization.Serializable
import org.osmdroid.util.GeoPoint
import java.util.*

@Entity(
    tableName = "place_info",
    primaryKeys = ["title", "type"]
)
@Serializable
data class Place (
    var id: String? = null,
    var title: String,
    var short_desc: String? = null,
    @TypeConverters(Converter::class)
    @Serializable(with = GeoPointSerializer::class)
    var geolocalisation: GeoPoint,
    var type: String,
    var photo: String? = null,
    var contact: String? = null
) {
    fun getIcon() = when (type.lowercase(Locale.getDefault())) {
        "batiment" -> R.drawable.ic_building
        "épicerie" -> R.drawable.ic_grocery
        "foodtruck" -> R.drawable.ic_foodtruck
        "triporteur" -> R.drawable.ic_foodtruck
        else -> R.drawable.ic_restaurant
    }
}



