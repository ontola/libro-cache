package tools.empathy.vocabularies

import tools.empathy.model.RdfProperty
import tools.empathy.model.add
import tools.empathy.model.langMap
import tools.empathy.serialization.dataSliceNonSuspend

val LibroData = dataSliceNonSuspend {
    add(
        RdfProperty(
            Libro.result,
            langMap {
                en = "Result"
                nl = "Resultaat"
                de = "Ergebnis"
            },
        ),
    )

    add(
        RdfProperty(
            Libro.message,
            langMap {
                en = "Message"
                nl = "Bericht"
                de = "Benachrichtigung"
            },
        ),
    )

    add(
        RdfProperty(
            Libro.Module.topologiesCount,
            langMap {
                en = "Topologies"
                nl = "Topologieën"
            },
        ),
    )

    add(
        RdfProperty(
            Libro.Module.type,
            langMap {
                en = "Module type"
                nl = "Type module"
            },
        ),
    )

    add(
        RdfProperty(
            Libro.Module.viewsCount,
            langMap {
                en = "View registrations"
                nl = "View registrations"
            },
        ),
    )
}
