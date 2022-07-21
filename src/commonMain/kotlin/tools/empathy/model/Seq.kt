package tools.empathy.model

import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Entity
import tools.empathy.serialization.Value
import tools.empathy.serialization.field
import tools.empathy.serialization.record
import tools.empathy.serialization.type

/**
 * An ordered list of values.
 */
data class Seq<T : Value>(
    override val id: Value.Id,
    val items: List<T>,
) : Entity

fun DataSlice.add(it: Seq<*>): Value.Id = record(it.id) {
    type("Seq")
    it.items.withIndex().forEach { (i, item) ->
        field("_$i") { add(item) }
    }
}
