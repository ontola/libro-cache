package tools.empathy.serialization

import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias FieldSet = MutableMap<String, List<Value>>

@Serializable(with = RecordSerializer::class)
data class Record(
    @SerialName("_id")
    val id: Value.Id,
    val fields: FieldSet = mutableMapOf(),
) {
    val entries
        get() = fields

    constructor(
        id: Url,
        fields: FieldSet = mutableMapOf(),
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
