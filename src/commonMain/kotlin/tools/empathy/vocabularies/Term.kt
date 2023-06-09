package tools.empathy.vocabularies

import tools.empathy.serialization.Value
import kotlin.reflect.KProperty

class Term(val term: String? = null) {
    operator fun getValue(thisRef: Vocab, property: KProperty<*>): Value.Id.Global =
        Value.Id.Global("${thisRef.vocab}${term ?: property.name}")
}
