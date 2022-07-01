package tools.empathy.model

import io.ontola.empathy.web.DataSlice
import io.ontola.empathy.web.Entity
import io.ontola.empathy.web.Value
import io.ontola.empathy.web.field
import io.ontola.empathy.web.id
import io.ontola.empathy.web.record
import io.ontola.empathy.web.s
import io.ontola.empathy.web.type

data class WebPage(
    override val id: Value.Id,
    val name: String,
    val text: String,
    val widgets: Value.Id,
) : Entity

fun DataSlice.add(it: WebPage): Value.Id = record(it.id) {
    type("WebPage")
    field("name") { s(it.name) }
    field("text") { s(it.text) }
    field("widgets") { id(it.widgets) }
}
