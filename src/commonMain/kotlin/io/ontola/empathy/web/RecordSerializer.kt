package io.ontola.empathy.web

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonObject

object RecordSerializer : KSerializer<Record> {
    class UnknownElementException : Exception()

    private val valueSerializer = Value.serializer()
    @OptIn(ExperimentalSerializationApi::class)
    private val valuesSerializer = ArraySerializer(valueSerializer)
    val serializer = MapSerializer(String.serializer(), valuesSerializer)
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: Record) {
        val data = buildMap {
            this["_id"] = JsonObject(value.id.toJsonElementMap())

            for ((field, values) in value.fields) {
                if (values.size == 1) {
                    this[field] = values[0].toJsonElementMap()
                } else {
                    this[field] = buildJsonArray {
                        for (v in values)
                            this.add(v.toJsonElementMap())
                    }
                }
            }
        }

        encoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(data))
    }

    override fun deserialize(decoder: Decoder): Record {
        val entries = decoder.decodeSerializableValue(JsonObject.serializer()).entries

        lateinit var id: Value.Id
        val fields = mutableMapOf<String, List<Value>>()

        for ((key, value) in entries) {
            if (key == "_id") {
                id = value.jsonObject.toId()
            } else if (value is JsonObject) {
                fields[key] = listOf(value.toValue())
            } else if (value is JsonArray) {
                fields[key] = value.map { it.jsonObject.toValue() }
            } else {
                throw UnknownElementException()
            }
        }

        return Record(
            id = id,
            fields = fields,
        )
    }
}
