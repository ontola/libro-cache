package tools.empathy.serialization.deep

import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tools.empathy.serialization.Value
import tools.empathy.serialization.toValue

typealias DeepFieldSet = MutableMap<String, List<Value>>

@Serializable(with = DeepRecordSerializer::class)
data class DeepRecord(
    @SerialName("_id")
    val id: Value.Id,
    val fields: DeepFieldSet = mutableMapOf(),
) {
    val entries
        get() = fields

    constructor(
        id: Url,
        fields: DeepFieldSet = mutableMapOf(),
    ) : this(id.toValue(), fields)

    operator fun get(field: String): List<Value>? {
        if (field == "_id") {
            throw Exception("Use id directly.")
        }

        return fields[field]
    }

    operator fun set(field: String, value: List<Value>) {
        this.fields[field] = value
    }
}
