package tools.empathy.studio

import io.ktor.http.Url
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

private object UrlAsStringSerializer : KSerializer<Url> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Url", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Url) {
        val string = value.toString()
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): Url {
        val string = decoder.decodeString()
        return Url(string)
    }
}

@OptIn(ExperimentalJsExport::class)
@Serializable
@JsExport
data class Publication(
    @Serializable(with = UrlAsStringSerializer::class)
    val startRoute: Url,
    val projectId: String,
    val distributionId: String,
)
