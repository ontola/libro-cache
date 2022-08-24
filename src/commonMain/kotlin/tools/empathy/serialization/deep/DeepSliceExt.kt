package tools.empathy.serialization.deep

import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.FieldSet
import tools.empathy.serialization.Record
import tools.empathy.serialization.Value
import tools.empathy.serialization.add
import tools.empathy.serialization.dataSliceNonSuspend

fun DeepSlice.flatten(): DataSlice {
    val rootNodes = this.values

    return dataSliceNonSuspend {
        rootNodes.forEach {
            it.walk { dr ->
                add(Record(dr.id, dr.fields.toFieldSet()))
            }
        }
    }
}

private fun DeepRecord.walk(op: (r: DeepRecord) -> Unit) {
    op(this)
    fields
        .values
        .flatten()
        .filterIsInstance<Value.NestedRecord>()
        .forEach {
            it.record.walk(op)
        }
}

private fun DeepFieldSet.toFieldSet(): FieldSet = mapValues {
    it.value.map { value ->
        if (value is Value.NestedRecord) {
            value.record.id
        } else {
            value
        }
    }
} as FieldSet
