package tools.empathy.model

import io.ontola.empathy.web.DataSlice
import io.ontola.empathy.web.Value
import tools.empathy.model.collection.PaginatedView
import tools.empathy.model.collection.add
import tools.empathy.vocabularies.ActivityStreams.totalItems

class CollectionBuilder {
    lateinit var id: Value.Id
    var type: CollectionType = CollectionType.Paginated()
    var title: String? = null
    var display: CollectionDisplay = CollectionDisplay.Default()
    var items = mutableListOf<Value>()

    val columns = mutableListOf<Value.Id>()
    val sortings = mutableListOf<CollectionSorting>()

    fun build(slice: DataSlice): Collection {
        val sortingsSeq = Seq(Value.Id.Local(), sortings.map { it.id })
        val columnsSeq = Seq(Value.Id.Local(), columns.toList())

        val viewSeq = Seq(
            Value.Id.Global("${id.id}?page=1#members"),
            items.toList(),
        )
        val view = PaginatedView(
            Value.Id.Global("${id.id}?page=1"),
            baseCollection = id,
            partOf = id,
            display = display,
            items = viewSeq.id,
            totalItems = items.size,
        )

        val collection = Collection(
            id = id,
            title = title,
            display = display,
            totalItems = items.size,
            serializedIriTemplate = "${id.id}{?display,page,page_size,sort%5B%5D*,table_type,type}",
            iriTemplateOpts = "",
            defaultType = CollectionType.Paginated(),
            collectionType = type,
            sortings = sortingsSeq.id,
            columns = columnsSeq.id,
            defaultView = view.id,
            first = view.id,
            last = view.id,
        )

        sortings.forEach { slice.add(it) }
        slice.add(sortingsSeq)
        slice.add(columnsSeq)
        slice.add(collection)
        slice.add(viewSeq)
        slice.add(view)

        return collection
    }
}

fun DataSlice.buildCollection(init: CollectionBuilder.() -> Unit): Collection {
    val builder = CollectionBuilder()
    builder.init()

    return builder.build(this)
}

fun CollectionBuilder.addColumn(column: Value.Id) {
    columns.add(column)
}

fun CollectionBuilder.addAll(items: List<Value>) {
    this.items.addAll(items)
}

fun CollectionBuilder.addSorting(sortKey: Value.Id, sortDirection: SortDirection) {
    val sorting = CollectionSorting(
        Value.Id.Local(),
        sortDirection = sortDirection,
        sortKey = sortKey,
    )

    sortings.add(sorting)
}
