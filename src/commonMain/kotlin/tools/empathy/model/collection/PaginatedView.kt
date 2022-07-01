package tools.empathy.model.collection

import io.ontola.empathy.web.DataSlice
import io.ontola.empathy.web.Entity
import io.ontola.empathy.web.Value
import io.ontola.empathy.web.field
import io.ontola.empathy.web.id
import io.ontola.empathy.web.int
import io.ontola.empathy.web.record
import io.ontola.empathy.web.type
import tools.empathy.model.CollectionDisplay
import tools.empathy.model.Seq
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
