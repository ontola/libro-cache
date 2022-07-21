package tools.empathy.serialization

import io.ktor.http.Url
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull

fun Value.toJsonElementMap(): JsonObject = when (this) {
    is Value.Id.Global -> buildJsonObject {
        put("type", JsonPrimitive("id"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
    }
    is Value.Id.Local -> buildJsonObject {
        put("type", JsonPrimitive("lid"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
    }
    is Value.Str -> buildJsonObject {
        put("type", JsonPrimitive("s"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
    }
    is Value.Bool -> buildJsonObject {
        put("type", JsonPrimitive("b"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
    }
    is Value.Int -> buildJsonObject {
        put("type", JsonPrimitive("i"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
    }
    is Value.Long -> buildJsonObject {
        put("type", JsonPrimitive("l"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
    }
    is Value.DateTime -> buildJsonObject {
        put("type", JsonPrimitive("dt"))
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
        "id" -> shortenedGlobalId(value, null)
        "lid" -> Value.Id.Local(value)
        "s" -> Value.Str(value)
        "b" -> Value.Bool(value)
        "i" -> Value.Int(value)
        "l" -> Value.Long(value)
        "dt" -> Value.DateTime(value)
        "ls" -> Value.LangString(value, (this["l"] as JsonPrimitive).content)
        "p" -> Value.Primitive(value, (this["dt"] as JsonPrimitive).content)
        else -> throw Exception("Unknown value type $type")
    }
}

fun JsonObject.toId(): Value.Id {
    val value = (this["v"] as JsonPrimitive).contentOrNull ?: throw Exception("No value for `v` key in Value")

    return when (val type = (this["type"] as JsonPrimitive).content) {
        "id" -> shortenedGlobalId(value, null)
        "lid" -> Value.Id.Local(value)
        else -> throw Exception("Unknown value type $type")
    }
}

fun Url.toValue(): Value.Id.Global = Value.Id.Global(this.toString())
