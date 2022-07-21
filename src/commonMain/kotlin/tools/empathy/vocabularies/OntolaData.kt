package tools.empathy.vocabularies

import tools.empathy.model.RdfProperty
import tools.empathy.model.add
import tools.empathy.model.langMap
import tools.empathy.serialization.dataSliceNonSuspend

val OntolaData = dataSliceNonSuspend {
    add(
        RdfProperty(
            Ontola.href,
            langMap {
                en = "Link"
                nl = "Link"
                de = "Verknüpfen"
            },
        ),
    )
}
