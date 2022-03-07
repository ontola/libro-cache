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
    @OptIn(ExperimentalSerializationApi::class)
    val valueSerializer = Value.serializer()
    val valuesSerializer = ArraySerializer(valueSerializer)
    val serializer = MapSerializer(String.serializer(), valuesSerializer)
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: Record) {
        val data = buildMap {
            this["_id"] = JsonObject(value.id.toJsonElementMap())

            for ((field, values) in value.fields) {
                this[field] = buildJsonArray {
                    for (v in values)
                        this.add(v.toJsonElementMap())
                }
            }
        }

        val test = JsonObject(data)
        encoder.encodeSerializableValue(JsonObject.serializer(), test)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Record {
        val entries = decoder.decodeSerializableValue(JsonObject.serializer()).entries

        lateinit var id: Value
        val fields = mutableMapOf<String, Array<Value>>()

        for ((key, value) in entries) {
            if (key == "_id") {
                id = value.jsonObject.toValue()
            } else {
                fields[key] = (value as JsonArray)
                    .map { it.jsonObject.toValue() }
                    .toTypedArray()
            }
        }

        return Record(
            id = id,
            fields = fields,
        )
    }
}
