package io.ontola.cache.util

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

fun URLBuilder.invalid(): Boolean {
    return host.contains(' ') || host.contains("\t")
}

object UrlSerializer : KSerializer<Url> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Url", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Url) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Url {
        val value = decoder.decodeString()

        return URLBuilder(value).apply {
            if (!value.contains(host) || !value.startsWith(protocol.name) || invalid()) {
                throw Exception("Malformed URL '$value'")
            }
        }.build()
    }
}
