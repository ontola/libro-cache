package tools.empathy.model

import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Entity
import tools.empathy.serialization.Value
import tools.empathy.serialization.field
import tools.empathy.serialization.id
import tools.empathy.serialization.int
import tools.empathy.serialization.record
import tools.empathy.serialization.type

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
