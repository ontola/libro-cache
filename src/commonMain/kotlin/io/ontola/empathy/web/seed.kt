package io.ontola.empathy.web

import io.ontola.rdf.hextuples.DataType
import io.ontola.rdf.hextuples.Hextuple
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull

const val supplantGraph = "http://purl.org/linked-delta/supplant"

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

fun JsonObject.toValue(): Value {
    val value = (this["v"] as JsonPrimitive).contentOrNull ?: throw Exception("No value for `v` key in Value")

    return when (val type = (this["type"] as JsonPrimitive).content) {
        "id" -> Value.GlobalId(value)
        "lid" -> Value.LocalId(value)
        "ls" -> Value.LangString(value, (this["l"] as JsonPrimitive).content)
        "p" -> Value.Primitive(value, (this["dt"] as JsonPrimitive).content)
        else -> throw Exception("Unknown value type $type")
    }
}

typealias DataSlice = Map<String, Record>

fun List<Hextuple?>.toSlice(): DataSlice = buildMap {
    for (hex in this@toSlice) {
        if (hex == null || hex.graph == "http://purl.org/link-lib/meta") continue

        if (hex.graph != supplantGraph) throw Error("Non-supplant statement: $hex")

        val record = this.getOrPut(hex.subject) {
            val id = if (hex.subject.startsWith("_"))
                Value.LocalId(hex.subject)
            else
                Value.GlobalId(hex.subject)
            Record(id)
        }
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
