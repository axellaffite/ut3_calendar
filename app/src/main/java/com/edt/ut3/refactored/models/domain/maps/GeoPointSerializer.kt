package com.edt.ut3.refactored.models.domain.maps

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.osmdroid.util.GeoPoint

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