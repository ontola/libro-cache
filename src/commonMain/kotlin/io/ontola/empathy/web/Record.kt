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

    operator fun get(field: String): Array<Value>? {
        if (field == "_id") {
            throw Exception("Use id directly.")
        }

        return fields[field]
    }

    operator fun set(field: String, value: Array<Value>) {
        this.fields[field] = value
    }
}
