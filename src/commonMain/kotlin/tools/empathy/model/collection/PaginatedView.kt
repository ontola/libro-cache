package tools.empathy.model.collection

import tools.empathy.model.CollectionDisplay
import tools.empathy.model.Seq
import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Entity
import tools.empathy.serialization.Value
import tools.empathy.serialization.field
import tools.empathy.serialization.id
import tools.empathy.serialization.int
import tools.empathy.serialization.record
import tools.empathy.serialization.type
import tools.empathy.vocabularies.ActivityStreams
import tools.empathy.vocabularies.Ontola

data class PaginatedView(
    override val id: Value.Id,

    val baseCollection: Value.Id,
    val display: CollectionDisplay,
    /** [Seq]<[Value.Id]> */
    val items: Value.Id,
    val next: Value.Id? = null,
    val totalItems: Int = 0,
    val partOf: Value.Id,
) : Entity

fun DataSlice.add(it: PaginatedView): Value.Id = record(it.id) {
    type(Ontola.CollectionView.id)

    field(Ontola.baseCollection) { id(it.baseCollection) }
    field(Ontola.collectionDisplay) { id(it.display) }
    field(ActivityStreams.partOf) { id(it.partOf) }
    field(ActivityStreams.items) { id(it.items) }
    field(ActivityStreams.next) { id(it.next) }
    field(ActivityStreams.totalItems) { int(it.totalItems) }
}
