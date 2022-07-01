package io.ontola.empathy.web

val localIds = (1..1_000_000)
val takenIds = mutableListOf<Int>()

fun nextId(): Int {
    var id = localIds.random()
    while (takenIds.contains(id)) {
        id = localIds.random()
    }
    takenIds.add(id)

    return id
}

suspend fun dataSlice(init: suspend DataSlice.() -> Unit): DataSlice {
    val slice = mutableMapOf<String, Record>()
    slice.init()
    return slice.toMap()
}

fun DataSlice.record(id: Value.Id = Value.Id.Local("_:${nextId()}"), init: Record.() -> Unit): Value.Id {
    val record = Record(id)
    record.init()

    (this as MutableMap<String, Record>)[id.value] = record

    return record.id
}

fun Record.type(value: String) = field("type") { id(value) }

fun Record.field(field: String, init: MutableList<Value>.() -> Unit) {
    val values = mutableListOf<Value>()
    values.init()
    fields[reverseSymbolMap[field] ?: field] = values
}

fun Record.field(name: Value.Id.Global, init: MutableList<Value>.() -> Unit) {
    field(name.id, init)
}

private fun <T : Any?> MutableList<Value>.addIfNotNull(item: T?, convert: (item: T) -> Value) {
    if (item == null)
        return

    add(convert(item))
}

fun MutableList<Value>.s(value: String?) = addIfNotNull(value) { Value.Str(it) }

fun MutableList<Value>.ls(value: String, lang: String) = add(Value.LangString(value, lang))

fun MutableList<Value>.int(value: String?) = addIfNotNull(value) { Value.Int(it) }

fun MutableList<Value>.int(value: Int?) = addIfNotNull(value) { Value.Int(it.toString(10)) }

fun MutableList<Value>.id(value: String?) = addIfNotNull(value) { Value.Id.Global(reverseSymbolMap[it] ?: it) }

fun MutableList<Value>.id(value: Value.Id?) = addIfNotNull(value) { it }

fun MutableList<Value>.id(value: Entity?) = addIfNotNull(value) { it.id }
