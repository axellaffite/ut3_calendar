package com.edt.ut3.backend.maps

import androidx.room.Entity
import androidx.room.TypeConverters
import com.edt.ut3.R
import com.edt.ut3.backend.database.Converter
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.osmdroid.util.GeoPoint
import java.util.Locale

@Entity(
    tableName = "place_info",
    primaryKeys = ["title", "type"]
)

data class Place(
    var id: String? = null,
    var title: String,
    var short_desc: String? = null,
    @TypeConverters(Converter::class)
    @field:JsonDeserialize(using = GeoPointDeserializer::class)
    @field:JsonSerialize(using = GeoPointSerializer::class)
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

class GeoPointDeserializer : JsonDeserializer<GeoPoint>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): GeoPoint {
        val node = p.codec.readTree<com.fasterxml.jackson.databind.JsonNode>(p)
        val coordinates = mutableListOf<Double>()

        node.forEach { coordinate ->
            coordinates.add(coordinate.asDouble())
        }

        return GeoPoint(coordinates[0], coordinates[1])
    }
}

class GeoPointSerializer : JsonSerializer<GeoPoint>() {
    override fun serialize(value: GeoPoint, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartArray()
        gen.writeNumber(value.latitude)
        gen.writeNumber(value.longitude)
        gen.writeEndArray()
    }
}