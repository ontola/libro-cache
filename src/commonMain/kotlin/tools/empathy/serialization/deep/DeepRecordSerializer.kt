package tools.empathy.serialization.deep

import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonObject
import tools.empathy.serialization.BaseRecordSerializer
import tools.empathy.serialization.Value
import tools.empathy.serialization.toJsonElementMap
import tools.empathy.serialization.toValue

object DeepRecordSerializer : BaseRecordSerializer<DeepRecord>() {
    override fun serialize(encoder: Encoder, value: DeepRecord) {
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

    override fun deserialize(decoder: Decoder): DeepRecord {
        return toRecord(decoder.decodeSerializableValue(JsonObject.serializer()))
    }

    override fun toValue(value: JsonObject): Value {
        val id = value["_id"]?.jsonObject
        if (id != null) {
            return Value.NestedRecord("").apply {
                record = toRecord(value)
            }
        }

        return value.toValue()
    }

    private fun toRecord(value: JsonObject): DeepRecord {
        val (id, fields) = value.mapFieldSet()

        return DeepRecord(
            id = id,
            fields = fields,
        )
    }
}
