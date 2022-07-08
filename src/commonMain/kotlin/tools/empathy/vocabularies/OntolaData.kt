package tools.empathy.vocabularies

import io.ontola.empathy.web.dataSliceNonSuspend
import tools.empathy.model.RdfProperty
import tools.empathy.model.add
import tools.empathy.model.langMap

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
