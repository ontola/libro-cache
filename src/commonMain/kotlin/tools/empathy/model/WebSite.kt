package tools.empathy.model

import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Entity
import tools.empathy.serialization.Value
import tools.empathy.serialization.field
import tools.empathy.serialization.id
import tools.empathy.serialization.record
import tools.empathy.serialization.s
import tools.empathy.serialization.type
import tools.empathy.vocabularies.Ontola

data class WebSite(
    override val id: Value.Id,
    val name: String,
    val text: String,
    val homepage: Value.Id,
    val navigationsMenu: Value.Id? = null,
) : Entity

fun DataSlice.add(it: WebSite): Value.Id = record(it.id) {
    type("WebSite")
    field("name") { s(it.name) }
    field("text") { s(it.text) }
    field("homepage") { id(it.homepage) }
    field(Ontola.navigationsMenu) { id(it.navigationsMenu) }
}
