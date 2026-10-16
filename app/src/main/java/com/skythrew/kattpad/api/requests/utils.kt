package com.skythrew.kattpad.api.requests

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date

/** java.time formatters are thread-safe; API timestamps are UTC, not device-local time. */
object SimpleDateSerializer : KSerializer<Date> {
    override val descriptor = PrimitiveSerialDescriptor("SimpleDate", PrimitiveKind.STRING)
    private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    override fun deserialize(decoder: Decoder): Date =
        Date.from(LocalDateTime.parse(decoder.decodeString(), format).toInstant(ZoneOffset.UTC))
    override fun serialize(encoder: Encoder, value: Date) =
        encoder.encodeString(format.format(value.toInstant().atOffset(ZoneOffset.UTC)))
}

object DateSerializer : KSerializer<Date> {
    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): Date = Date.from(Instant.parse(decoder.decodeString()))
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeString(value.toInstant().toString())
}
