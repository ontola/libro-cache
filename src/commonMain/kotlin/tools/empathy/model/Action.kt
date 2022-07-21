package tools.empathy.model

import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Entity
import tools.empathy.serialization.Value
import tools.empathy.serialization.field
import tools.empathy.serialization.id
import tools.empathy.serialization.record
import tools.empathy.serialization.s
import tools.empathy.serialization.type
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
