package tools.empathy.model

import io.ontola.empathy.web.DataSlice
import io.ontola.empathy.web.Entity
import io.ontola.empathy.web.Value
import io.ontola.empathy.web.field
import io.ontola.empathy.web.id
import io.ontola.empathy.web.int
import io.ontola.empathy.web.record
import io.ontola.empathy.web.type

data class Widget(
    override val id: Value.Id,
    val order: Int,
    val topology: Value.Id,
    val widgetSize: Int,
    val view: Value.Id,
    val widgetResource: Value.Id,
) : Entity

fun DataSlice.add(it: Widget): Value.Id = record(it.id) {
    type("Widget")
    field("order") { int(it.order) }
    field("topology") { id(it.topology) }
    field("widgetSize") { int(it.widgetSize) }
    field("view") { id(it.view) }
    field("widgetResource") { id(it.widgetResource) }
}
