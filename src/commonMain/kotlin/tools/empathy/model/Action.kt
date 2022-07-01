package tools.empathy.model

import io.ontola.empathy.web.DataSlice
import io.ontola.empathy.web.Entity
import io.ontola.empathy.web.Value
import io.ontola.empathy.web.field
import io.ontola.empathy.web.id
import io.ontola.empathy.web.record
import io.ontola.empathy.web.s
import io.ontola.empathy.web.type
import tools.empathy.vocabularies.RdfSyntax

data class Action(
    override val id: Value.Id,
    val specificType: Value.Id? = null,
    val name: String,
    val text: String,
    val target: Value.Id,
    val actionStatus: Value.Id,
) : Entity

fun DataSlice.add(it: Action): Value.Id = record(it.id) {
    type("Action")
    it.specificType?.let { type ->
        field(RdfSyntax.type) { id(type) }
    }
    field("name") { s(it.name) }
    field("text") { s(it.text) }
    field("target") { id(it.target) }
    field("actionStatus") { id(it.actionStatus) }
}
