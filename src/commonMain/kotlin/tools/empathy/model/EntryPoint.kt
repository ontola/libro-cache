package tools.empathy.model

import io.ontola.empathy.web.DataSlice
import io.ontola.empathy.web.Entity
import io.ontola.empathy.web.Value
import io.ontola.empathy.web.field
import io.ontola.empathy.web.id
import io.ontola.empathy.web.record
import io.ontola.empathy.web.s
import io.ontola.empathy.web.type

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
