package tools.empathy.vocabularies

import tools.empathy.model.RdfProperty
import tools.empathy.model.add
import tools.empathy.model.langMap
import tools.empathy.serialization.dataSliceNonSuspend

val SchemaData = dataSliceNonSuspend {
    add(
        RdfProperty(
            Schema.name,
            langMap {
                en = "Name"
                nl = "Naam"
                de = "Name"
            },
        ),
    )
    add(
        RdfProperty(
            Schema.text,
            langMap {
                en = "Text"
                nl = "Tekst"
                de = "Text"
            },
        ),
    )
}
