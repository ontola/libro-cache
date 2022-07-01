package tools.empathy.model

import io.ontola.empathy.web.DataSlice
import io.ontola.empathy.web.Entity
import io.ontola.empathy.web.Value
import io.ontola.empathy.web.field
import io.ontola.empathy.web.record
import io.ontola.empathy.web.type

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
