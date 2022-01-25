package io.ontola.empathy.web

import io.ontola.rdf.hextuples.DataType
import io.ontola.rdf.hextuples.Hextuple
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject

@Serializable
sealed class Value(
    @Transient
    val value: String = "",
) {
    @Serializable
    @SerialName("id")
    data class GlobalId(
        @SerialName("v")
        val id: String,
    ) : Value(id)

    @Serializable
    @SerialName("lid")
    data class LocalId(
        @SerialName("v")
        val id: String,
    ) : Value(id)

    @Serializable
    @SerialName("p")
    data class Primitive(
        @SerialName("v")
        val lexical: String,
        @SerialName("dt")
        val dataType: String,
    ) : Value(lexical)

    @Serializable
    @SerialName("ls")
    data class LangString(
        @SerialName("v")
        val lexical: String,
        @SerialName("l")
        val lang: String,
    ) : Value(lexical)
}

fun Value.toJsonElementMap(): JsonObject = when (this) {
    is Value.GlobalId -> buildJsonObject {
        put("type", JsonPrimitive("id"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
    }
    is Value.LocalId -> buildJsonObject {
        put("type", JsonPrimitive("lid"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
    }
    is Value.LangString -> buildJsonObject {
        put("type", JsonPrimitive("ls"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
        put("l", JsonPrimitive(this@toJsonElementMap.lang))
    }
    is Value.Primitive -> buildJsonObject {
        put("type", JsonPrimitive("p"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
        put("dt", JsonPrimitive(this@toJsonElementMap.dataType))
    }
}

@Serializable(with = RecordSerializer::class)
data class Record(
    @SerialName("_id")
    val id: Value,
    val fields: MutableMap<String, Array<Value>> = mutableMapOf(),
) {
    val entries
        get() = fields

    operator fun get(key: String): Array<Value>? {
        if (key == "_id") {
            throw Exception("Use id directly.")
        }

        return fields[key]
    }

    operator fun set(predicate: String, value: Array<Value>) {
        this.fields[predicate] = value
    }
}

typealias DataSlice = Map<String, Record>

fun List<Hextuple?>.toSlice(): DataSlice = buildMap {
    for (hex in this@toSlice) {
        if (hex == null || hex.graph == "http://purl.org/link-lib/meta") continue

        if (hex.graph != "http://purl.org/link-lib/supplant") throw Error("Non-supplant statement: $hex")

        val record = this.getOrPut(hex.subject) { Record(Value.GlobalId(hex.subject)) }
        val field = record.entries.getOrPut(hex.predicate) { arrayOf() }
        val value = hex.toValue()
        record[hex.predicate] = arrayOf(*field, value)

        put(hex.subject, record)
    }
}

fun Hextuple.toValue(): Value = when (datatype) {
    is DataType.GlobalId -> Value.GlobalId(value)
    is DataType.LocalId -> Value.LocalId(value)
    else ->
        if (language == "")
            Value.Primitive(value, datatype.value())
        else
            Value.LangString(value, language)
}

object RecordSerializer : KSerializer<Record> {
    @OptIn(ExperimentalSerializationApi::class)
    val valuesSerializer = ArraySerializer(Value.serializer())
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

    override fun deserialize(decoder: Decoder): Record = serializer
        .deserialize(decoder)
        .let {
            Record(
                id = it["_id"]!![0],
                fields = it.toMutableMap(),
            )
        }
}
