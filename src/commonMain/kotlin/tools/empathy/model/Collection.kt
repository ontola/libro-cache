package tools.empathy.model

import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Entity
import tools.empathy.serialization.Value
import tools.empathy.serialization.field
import tools.empathy.serialization.id
import tools.empathy.serialization.int
import tools.empathy.serialization.record
import tools.empathy.serialization.s
import tools.empathy.serialization.type
import tools.empathy.vocabularies.ActivityStreams
import tools.empathy.vocabularies.LinkLib
import tools.empathy.vocabularies.Ontola
import tools.empathy.vocabularies.Schema

sealed class CollectionDisplay(override val id: Value.Id) : Entity {
    class Default : CollectionDisplay(Value.Id.Global(Ontola.collectionDisplayDefault.id))

    class Grid : CollectionDisplay(Value.Id.Global(Ontola.collectionDisplayGrid.id))

    class Card : CollectionDisplay(Value.Id.Global(Ontola.collectionDisplayCard.id))

    class SettingsTable : CollectionDisplay(Value.Id.Global(Ontola.collectionDisplaySettingsTable.id))

    class Table : CollectionDisplay(Value.Id.Global(Ontola.collectionDisplayTable.id))
}

sealed class CollectionType(override val id: Value.Id) : Entity {
    class Paginated : CollectionType(Value.Id.Global(Ontola.collectionTypePaginated.id))

    class Infinite : CollectionType(Value.Id.Global(Ontola.collectionTypeInfinite.id))
}

enum class SortDirection(val dir: String) {
    Asc("asc"),
    Desc("desc"),
}

data class CollectionSorting(
    override val id: Value.Id,
    val sortDirection: SortDirection,
    val sortKey: Value.Id,
) : Entity

data class FilterField(
    override val id: Value.Id,
    val visible: Boolean,
    val filterOptions: List<Value.Id>,
    val filterKey: Value.Id,
    val partOf: Value.Id,
) : Entity

data class FilterOption(
    override val id: Value.Id,
    val filterKey: Value.Id,
    val filterValue: Value,
) : Entity

data class Collection(
    override val id: Value.Id,

    val baseUrl: Value.Id? = null,
    val title: String? = null,
    val totalItems: Int? = null,
    val serializedIriTemplate: String? = null,
    val iriTemplateOpts: String? = null,
    val defaultType: CollectionType = CollectionType.Paginated(),
    val display: CollectionDisplay? = null,
    val callToAction: Value.Id? = null,
    /** [Seq] of [Value.Id] */
    val columns: Value.Id? = null,
    val collectionType: CollectionType = CollectionType.Paginated(),
    val gridMaxColumns: Int = 3,
    val sortOptions: Value.Id? = null,
    val view: Value.Id? = null,
    val first: Value.Id? = null,
    val last: Value.Id? = null,

    /** has one */

    val unfilteredCollection: Value.Id? = null,
    val partOf: Value.Id? = null,
    val defaultView: Value.Id? = null,

    /** has many */

    val filterFields: Seq<Value.Id>? = null,
    val filters: Value.Id? = null,
    val sortings: Value.Id? = null,
) : Entity

fun DataSlice.add(it: Collection): Value.Id = record(it.id) {
    type(Ontola.Collection.id)
    type(ActivityStreams.Collection.id)
    field(Schema.url) { id(it.baseUrl) }
    field(ActivityStreams.name) { s(it.title) }
    field(ActivityStreams.totalItems) { int(it.totalItems) }
    field(ActivityStreams.first) { id(it.first) }
    field(ActivityStreams.last) { id(it.last) }
    field(Ontola.iriTemplate) { id(it.serializedIriTemplate) }
    field(Ontola.iriTemplateOpts) { id(it.iriTemplateOpts) }
    field(Ontola.defaultType) { id(it.defaultType) }
    field(Ontola.collectionDisplay) { id(it.display?.id) }
    field(Ontola.callToAction) { id(it.callToAction) }
    field(Ontola.columns) { id(it.columns) }
    field(Ontola.collectionType) { id(it.collectionType) }
    field(Ontola.maxColumns) { int(it.gridMaxColumns) }
    field(Ontola.sortOptions) { id(it.sortOptions) }
    field(LinkLib.view) { id(it.view) }
    field(Ontola.baseCollection) { id(it.unfilteredCollection) }
    field(Schema.isPartOf) { id(it.partOf) }
    field(Ontola.pages) { id(it.defaultView) }
    field(Ontola.filterFields) { id(it.filterFields) }
    field(Ontola.collectionFilter) { id(it.filters) }
    field(Ontola.collectionSorting) { id(it.sortings) }
}

fun DataSlice.add(it: CollectionSorting): Value.Id = record(it.id) {
    type(Ontola.CollectionSortingClass.id)
    field(Ontola.sortDirection) { s(it.sortDirection.dir) }
    field(Ontola.sortKey) { id(it.sortKey) }
}
