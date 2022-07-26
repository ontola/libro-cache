package tools.empathy.libro.server.landing

import tools.empathy.model.Seq
import tools.empathy.model.WebPage
import tools.empathy.model.Widget
import tools.empathy.model.add
import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Value
import tools.empathy.serialization.record
import tools.empathy.serialization.type
import tools.empathy.vocabularies.Libro

internal fun DataSlice.modulesPage(): Value.Id {
    return add(
        WebPage(
            id = Value.Id.Global("https://localhost/modules"),
            name = "Modules",
            text = "",
            widgets = add(
                Seq(
                    Value.Id.Global("https://localhost/modules/widgets"),
                    listOf(
                        *libroWidgets(),
                    ),
                ),
            ),
        ),
    )
}

private fun DataSlice.libroWidgets(): Array<Value.Id> {
    val modules = record {
        type(Libro.Boostrap.ModulesList)
    }

    val topologies = record {
        type(Libro.Boostrap.TopologiesList)
    }

    return arrayOf(
        add(
            Widget(
                id = Value.Id.Global("https://localhost/home/widgets/bootstrap/modules"),
                order = 1,
                topology = Value.Id.Global("https://ns.ontola.io/libro/topologies/grid"),
                widgetSize = 2,
                view = Value.Id.Global("https://argu.nl/enums/widgets/view#preview_view"),
                widgetResource = modules,
            ),
        ),
        add(
            Widget(
                id = Value.Id.Global("https://localhost/home/widgets/bootstrap/topologies"),
                order = 3,
                topology = Value.Id.Global("https://ns.ontola.io/libro/topologies/card"),
                widgetSize = 1,
                view = Value.Id.Global("https://argu.nl/enums/widgets/view#preview_view"),
                widgetResource = topologies,
            ),
        )
    )
}
