package io.ontola.empathy.web

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
