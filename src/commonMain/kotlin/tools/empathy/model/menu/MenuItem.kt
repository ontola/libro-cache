package tools.empathy.model.menu

import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Entity
import tools.empathy.serialization.Value
import tools.empathy.serialization.field
import tools.empathy.serialization.id
import tools.empathy.serialization.int
import tools.empathy.serialization.record
import tools.empathy.serialization.s
import tools.empathy.serialization.type
import tools.empathy.vocabularies.Ontola
import tools.empathy.vocabularies.Schema

data class MenuItem(
    override val id: Value.Id,
    /** [tools.empathy.model.Seq]<[MenuItem]> */
    val menuItems: Value.Id? = null,
    val encodingFormat: String? = null,
    val image: Value.Id? = null,
    val isPartOf: Value.Id? = null,
    val name: String? = null,
    val customImage: Value.Id? = null,
    val edge: Value.Id? = null,
    val order: Int? = null,
    val targetType: Value.Id? = null,
    val href: Value.Id? = null,
) : Entity

fun DataSlice.add(it: MenuItem): Value.Id = record(it.id) {
    type(Ontola.MenuItem)

    field(Ontola.menuItems) { id(it.menuItems) }
    field(Schema.encodingFormat) { s(it.encodingFormat) }
    field(Schema.image) { id(it.image) }
    field(Schema.isPartOf) { id(it.isPartOf) }
    field(Schema.name) { id(it.name) }
    field("https://argu.co/ns/core#customImage") { id(it.customImage) }
    field("https://argu.co/ns/core#edge") { id(it.edge) }
    field("https://argu.co/ns/core#order") { int(it.order) }
    field("https://argu.co/ns/core#targetType") { id(it.targetType) }
    field(Ontola.href) { id(it.href) }
}
