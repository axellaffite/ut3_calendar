package com.edt.ut3.backend.maps

import androidx.room.Entity
import androidx.room.TypeConverters
import com.edt.ut3.R
import com.edt.ut3.backend.database.Converter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
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



object GeoPointSerializer: KSerializer<GeoPoint> {

    private val serializer = ListSerializer(Double.serializer())

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("GeoPoint", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): GeoPoint {
        val localisation = decoder.decodeSerializableValue(serializer)
        return GeoPoint(localisation[0], localisation[1])
    }

    override fun serialize(encoder: Encoder, value: GeoPoint) {
        val localisation = listOf(value.latitude, value.longitude)
        encoder.encodeSerializableValue(serializer, localisation)
    }

}