package tools.empathy.vocabularies

import io.ontola.empathy.web.dataSliceNonSuspend
import tools.empathy.model.RdfProperty
import tools.empathy.model.add
import tools.empathy.model.langMap


val LibroData = dataSliceNonSuspend {
    add(RdfProperty(Libro.result, langMap {
        en = "Result"
        nl = "Resultaat"
        de = "Ergebnis"
    }))

    add(RdfProperty(Libro.message, langMap {
        en = "Message"
        nl = "Bericht"
        de = "Benachrichtigung"
    }))
}
