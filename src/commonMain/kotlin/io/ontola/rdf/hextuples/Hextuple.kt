package io.ontola.rdf.hextuples

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = HextupleSerializer::class)
data class Hextuple(
    val subject: String,
    val predicate: String,
    val value: String,
    val datatype: DataType,
    val language: String,
    val graph: String,
) {
    fun toArray(): Array<String> = arrayOf(
        subject,
        predicate,
        value,
        datatype.value(),
        language,
        graph,
    )

    companion object {
        fun fromArray(data: Array<String>) = Hextuple(
            data[HexPosition.Subject.ordinal],
            data[HexPosition.Predicate.ordinal],
            data[HexPosition.Value.ordinal],
            DataType.fromValue(data[HexPosition.DataType.ordinal]),
            data[HexPosition.Language.ordinal],
            data[HexPosition.Graph.ordinal],
        )
    }
}

object HextupleSerializer : KSerializer<Hextuple> {
    @OptIn(ExperimentalSerializationApi::class)
    val serializer = ListSerializer(String.serializer())
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: Hextuple) {
        val list = encoder.beginCollection(descriptor, 6)
        value.toArray().forEachIndexed { index, element ->
            list.encodeStringElement(descriptor, index, element)
        }
        list.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): Hextuple = serializer.deserialize(decoder).let {
        val datatype = DataType.fromValue(it[3])
        val objValue = if (datatype is DataType.LocalId) {
            it[2].trimStart('_', ':')
        } else {
            it[2]
        }

        Hextuple(
            subject = it[0],
            predicate = it[1],
            value = objValue,
            datatype = datatype,
            language = it[4],
            graph = it[5],
        )
    }
}
