package tools.empathy.model

import io.ontola.empathy.web.DataSlice
import io.ontola.empathy.web.Entity
import io.ontola.empathy.web.Value
import io.ontola.empathy.web.field
import io.ontola.empathy.web.ls
import io.ontola.empathy.web.record
import io.ontola.empathy.web.type
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
