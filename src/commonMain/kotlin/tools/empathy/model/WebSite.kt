package tools.empathy.model

import io.ontola.empathy.web.DataSlice
import io.ontola.empathy.web.Entity
import io.ontola.empathy.web.Value
import io.ontola.empathy.web.field
import io.ontola.empathy.web.id
import io.ontola.empathy.web.record
import io.ontola.empathy.web.s
import io.ontola.empathy.web.type
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
