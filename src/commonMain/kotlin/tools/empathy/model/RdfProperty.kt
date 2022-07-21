package tools.empathy.model

import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Entity
import tools.empathy.serialization.Value
import tools.empathy.serialization.field
import tools.empathy.serialization.ls
import tools.empathy.serialization.record
import tools.empathy.serialization.type
import tools.empathy.vocabularies.RdfSchema
import tools.empathy.vocabularies.RdfSyntax

data class RdfProperty(
    override val id: Value.Id,
    val name: LangMap,
) : Entity

fun DataSlice.add(it: RdfProperty): Value.Id = record(it.id) {
    type(RdfSyntax.Property)
    field(RdfSchema.label) { ls(it.name) }
}
