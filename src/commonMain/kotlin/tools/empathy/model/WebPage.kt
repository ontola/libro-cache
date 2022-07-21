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
import tools.empathy.vocabularies.Schema

data class WebPage(
    override val id: Value.Id,
    val name: String,
    val text: String,
    val image: Value.Id? = null,
    val coverPhoto: Value.Id? = null,
    val widgets: Value.Id,
) : Entity

fun DataSlice.add(it: WebPage): Value.Id = record(it.id) {
    type(Schema.WebPage)
    field("name") { s(it.name) }
    field("text") { s(it.text) }
    field(Schema.image) { id(it.image) }
    field(Ontola.coverPhoto) { id(it.coverPhoto) }
    field("widgets") { id(it.widgets) }
}
