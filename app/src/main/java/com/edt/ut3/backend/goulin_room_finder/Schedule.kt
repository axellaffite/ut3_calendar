package com.edt.ut3.backend.goulin_room_finder

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.text.SimpleDateFormat
import java.util.*

/**
 * Represents a schedule in
 * the Goulin's API.
 *
 * @property start The beginning of the schedule
 * @property end The ending of the schedule
 */
@Serializable
data class Schedule(
    @Serializable(with = DateSerializer::class)
    val start: Date,
    @Serializable(with = DateSerializer::class)
    val end: Date
)

object DateSerializer : KSerializer<Date> {

    private const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    private val dateFormatter = SimpleDateFormat(DATE_FORMAT).apply {
        timeZone = TimeZone.getTimeZone("PST")
    }

    override fun deserialize(decoder: Decoder): Date {
        val date = decoder.decodeString()
        return dateFormatter.parse(date)!!
    }

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeString(dateFormatter.format(value))
    }

}