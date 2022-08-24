package tools.empathy.serialization

import tools.empathy.model.LangMap

val localIds = (1..1_000_000)
val takenIds = mutableListOf<Int>()

/**
 * DSL to build a [DataSlice].
 */
suspend fun dataSlice(init: suspend DataSlice.() -> Unit): DataSlice {
    val slice = mutableMapOf<String, Record>()
    slice.init()
    return slice.toMap()
}

fun dataSliceNonSuspend(init: DataSlice.() -> Unit): DataSlice {
    val slice = mutableMapOf<String, Record>()
    slice.init()
    return slice.toMap()
}

private fun nextId(): Int {
    var id = localIds.random()
    while (takenIds.contains(id)) {
        id = localIds.random()
    }
    takenIds.add(id)

    return id
}

/**
 * Construct and add a [Record] to the current [DataSlice].
 * Note that previous data will be overridden.
 */
fun DataSlice.record(id: Value.Id = Value.Id.Local("_:${nextId()}"), init: Record.() -> Unit): Value.Id {
    val record = Record(id)
    record.init()

    add(record)

    return record.id
}

fun DataSlice.add(record: Record) {
    (this as MutableMap<String, Record>)[record.id.value] = record
}

/**
 * Set the type field for the [Record].
 */
fun Record.type(value: String) = field("type") { id(value) }

/**
 * Set the type field for the [Record].
 */
fun Record.type(value: Value.Id) = field("type") { id(value) }

/**
 * Add a [field] for the [Record]. Use one of the property setters in the callback.
 *
 * @receiver The [Record] to modify.
 * @see [dt] To add a Value.DateTime
 * @see [s] To add a String
 * @see [ls] To add a Value.LangString
 * @see [int] To add an Int
 * @see [id] To add a link to another record
 */
fun Record.field(field: String, init: MutableList<Value>.() -> Unit) {
    val values = mutableListOf<Value>()
    values.init()
    fields[reverseSymbolMap[field] ?: field] = values
}

/**
 * Add a [field] for the [Record]. Use one of the property setters in the callback.
 *
 * @receiver The [Record] to modify.
 * @see [dt] To add a Value.DateTime
 * @see [s] To add a String
 * @see [ls] To add a Value.LangString
 * @see [int] To add an Int
 * @see [id] To add a link to another record
 */
fun Record.field(name: Value.Id.Global, init: MutableList<Value>.() -> Unit) {
    field(name.id, init)
}

private fun <T : Any?> MutableList<Value>.addIfNotNull(item: T?, convert: (item: T) -> Value) {
    if (item == null)
        return

    add(convert(item))
}

/**
 * Adds a [Value.DateTime] from a [String] to the current property when it's not null.
 * @see [field]
 */
fun MutableList<Value>.dt(value: String?) = addIfNotNull(value) { Value.DateTime(it) }

/**
 * Adds a [Value.DateTime] to the current property when it's not null.
 * @see [field]
 */
fun MutableList<Value>.dt(value: Value.DateTime?) = addIfNotNull(value) { it }

/**
 * Adds a [Value.Str] from a [String] to the current property when it's not null.
 * @see [field]
 */
fun MutableList<Value>.s(value: String?) = addIfNotNull(value) { Value.Str(it) }

/**
 * Adds a [Value.LangString] from a [LangMap] to the current property when it's not null.
 * @see [field]
 */
fun MutableList<Value>.ls(values: LangMap) = values.forEach { (_, value) -> add(value) }

/**
 * Adds a [Value.Str] from a [String] to the current property when it's not null.
 * @see [field]
 */
fun MutableList<Value>.int(value: String?) = addIfNotNull(value) { Value.Int(it) }

/**
 * Adds a [Value.Int] from an [Int] to the current property when it's not null.
 * @see [field]
 */
fun MutableList<Value>.int(value: Int?) = addIfNotNull(value) { Value.Int(it.toString(10)) }

/**
 * Adds a [Value.Id] from a [String] to the current property when it's not null.
 * @see [field]
 */
fun MutableList<Value>.id(value: String?) = addIfNotNull(value) { Value.Id.Global(reverseSymbolMap[it] ?: it) }

/**
 * Adds a [Value.Id] to the current property when it's not null.
 * @see [field]
 */
fun MutableList<Value>.id(value: Value.Id?) = addIfNotNull(value) { it }

/**
 * Adds an [Value.Id] from an [Entity] to the current property when it's not null.
 * @see [field]
 */
fun MutableList<Value>.id(value: Entity?) = addIfNotNull(value) { it.id }
