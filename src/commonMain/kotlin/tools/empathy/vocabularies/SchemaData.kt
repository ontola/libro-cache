package tools.empathy.vocabularies

import io.ontola.empathy.web.dataSliceNonSuspend
import tools.empathy.model.RdfProperty
import tools.empathy.model.add
import tools.empathy.model.langMap


val SchemaData = dataSliceNonSuspend {
    add(RdfProperty(Schema.name, langMap {
        en = "Name"
        nl = "Naam"
        de = "Name"
    }))
    add(RdfProperty(Schema.text, langMap {
        en = "Text"
        nl = "Tekst"
        de = "Text"
    }))
}
