package tools.empathy.model

import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Entity
import tools.empathy.serialization.Value
import tools.empathy.serialization.field
import tools.empathy.serialization.id
import tools.empathy.serialization.record
import tools.empathy.serialization.s
import tools.empathy.serialization.type

data class EntryPoint(
    override val id: Value.Id,
    val name: String,
    val isPartOf: Value.Id,
    val httpMethod: String,
    val url: Value.Id,
) : Entity

fun DataSlice.add(it: EntryPoint): Value.Id = record(it.id) {
    type("EntryPoint")
    field("name") { s(it.name) }
    field("isPartOf") { id(it.isPartOf) }
    field("httpMethod") { s(it.httpMethod) }
    field("url") { id(it.url) }
}
