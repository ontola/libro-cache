package tools.empathy.model

import io.ontola.empathy.web.DataSlice
import io.ontola.empathy.web.Entity
import io.ontola.empathy.web.Value
import io.ontola.empathy.web.field
import io.ontola.empathy.web.id
import io.ontola.empathy.web.record
import io.ontola.empathy.web.s
import io.ontola.empathy.web.type
import tools.empathy.vocabularies.ActivityStreams.id
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
