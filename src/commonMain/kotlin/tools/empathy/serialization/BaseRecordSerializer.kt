package tools.empathy.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ArraySerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

abstract class BaseRecordSerializer<K> : KSerializer<K> {
    class UnknownElementException : Exception()

    private val valueSerializer = Value.serializer()

    @OptIn(ExperimentalSerializationApi::class)
    private val valuesSerializer = ArraySerializer(valueSerializer)
    val serializer = MapSerializer(String.serializer(), valuesSerializer)
    override val descriptor: SerialDescriptor = serializer.descriptor

    protected fun JsonObject.mapFieldSet(): Pair<Value.Id, FieldSet> {
        lateinit var id: Value.Id
        val fields = mutableMapOf<String, List<Value>>()

        for ((key, value) in entries) {
            if (key == "_id") {
                id = value.jsonObject.toId()
            } else if (value is JsonObject) {
                fields[key] = listOf(toValue(value))
            } else if (value is JsonArray) {
                fields[key] = value.map { it.jsonObject.toValue() }
            } else {
                throw UnknownElementException()
            }
        }

        return Pair(id, fields)
    }

    protected open fun toValue(value: JsonObject): Value {
        return value.toValue()
    }
}
