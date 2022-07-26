package tools.empathy.libro.server.landing

import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Value
import tools.empathy.serialization.record
import tools.empathy.serialization.type
import tools.empathy.vocabularies.Libro

internal fun DataSlice.browserPage(): Value.Id {
    return record(Value.Id.Global("/browser")) {
        type(Libro.Boostrap.Browser)
    }
}
