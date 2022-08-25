package tools.empathy.libro.server.landing

import tools.empathy.libro.server.health.Check
import tools.empathy.libro.server.health.CheckResult
import tools.empathy.libro.server.health.humanStatus
import tools.empathy.libro.server.tenantization.TenantDescription
import tools.empathy.model.Action
import tools.empathy.model.CollectionDisplay
import tools.empathy.model.EntryPoint
import tools.empathy.model.ImageObject
import tools.empathy.model.Seq
import tools.empathy.model.SortDirection
import tools.empathy.model.WebPage
import tools.empathy.model.Widget
import tools.empathy.model.add
import tools.empathy.model.addAll
import tools.empathy.model.addColumn
import tools.empathy.model.addSorting
import tools.empathy.model.buildCollection
import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Value
import tools.empathy.serialization.field
import tools.empathy.serialization.id
import tools.empathy.serialization.record
import tools.empathy.serialization.s
import tools.empathy.serialization.type
import tools.empathy.vocabularies.Libro
import tools.empathy.vocabularies.Ontola
import tools.empathy.vocabularies.Schema

internal const val MdLineBreak = "  "

internal fun DataSlice.homePage(
    tenants: List<TenantDescription>,
    backendFound: Boolean,
    defaultServicePort: String?,
    versions: VersionSet,
    healthChecks: List<Check>,
    isDev: Boolean,
): Value.Id {
    val logo = add(
        ImageObject(
            Value.Id.Local(),
            description = "Libro logo",
            imgUrl64x64 = Value.Id.Global("/f_assets/libro/libro-logo.svg"),
            imgUrl256x256 = Value.Id.Global("/f_assets/libro/libro-logo.svg"),
            imgUrl1500x2000 = Value.Id.Global("/f_assets/libro/libro-logo.svg"),
        ),
    )
    val cover = add(
        ImageObject(
            Value.Id.Local(),
            description = "Libro cover",
            imgUrl1500x2000 = Value.Id.Global("/f_assets/libro/libro-cover.webp"),
        ),
    )

    return add(
        WebPage(
            id = Value.Id.Global("/home"),
            name = "Welcome to Libro",
            text = """
                The information on this page is cached until a full reload.
                
                API version: ${versions.api ?: "Not detected"}$MdLineBreak
                Server version: ${versions.server}$MdLineBreak
                Client version: ${versions.client}
            """.trimIndent(),
            image = logo,
            coverPhoto = cover,
            widgets = add(
                Seq(
                    Value.Id.Global("/home/widgets"),
                    listOfNotNull(
                        tenantWidget(tenants),
                        docsWidget(isDev),
                        if (!backendFound) noBackendWidget(defaultServicePort) else null,
                        studioWidget(),
                        healthWidget(healthChecks),
                    ),
                ),
            ),
        ),
    )
}

private fun DataSlice.docsWidget(isDev: Boolean): Value.Id {
    val docsUrl = if (isDev) {
        Value.Id.Global("/libro/docs/")
    } else {
        Value.Id.Global("https://ontola.gitlab.io/cache/")
    }

    val entryPoint = Value.Id.Global("/home/widgets/docs/list/entryPoint")

    val action = add(
        Action(
            id = Value.Id.Global("/home/widgets/docs/list/action"),
            specificType = Schema.CreateAction,
            name = "Documentation",
            text = "Open the libro-server reference",
            target = entryPoint,
            actionStatus = Value.Id.Global("http://schema.org/PotentialActionStatus"),
        ),
    )

    add(
        EntryPoint(
            id = entryPoint,
            name = "Open",
            isPartOf = action,
            httpMethod = "GET",
            formTarget = "_blank",
            url = docsUrl,
        ),
    )

    return add(
        Widget(
            id = Value.Id.Global("/home/widgets/docs"),
            order = 0,
            topology = Value.Id.Global("https://ns.ontola.io/libro/topologies/grid"),
            widgetSize = 1,
            view = Value.Id.Global("https://argu.nl/enums/widgets/view#preview_view"),
            widgetResource = entryPoint,
        ),
    )
}

private fun DataSlice.noBackendWidget(defaultServicePort: String?): Value.Id {
    val info = record {
        type(Schema.CreativeWork)
        field(Schema.name) { s("No backend found") }
        field(Schema.text) {
            val portHelper = if (defaultServicePort == null) "" else " (`$defaultServicePort`)"

            s(
                """
                No data service detected. Check whether the `DEFAULT_SERVICE_PORT`$portHelper is correctly set and the server is running on that port.
                
                See the [linked_rails framework](https://github.com/ontola/linked_rails) to build a Libro compatible service. 
                """.trimIndent(),
            )
        }
    }

    return add(
        Widget(
            id = Value.Id.Global("/home/widgets/noBackend"),
            order = 1,
            topology = Value.Id.Global("https://ns.ontola.io/libro/topologies/container"),
            widgetSize = 2,
            view = Value.Id.Global("https://argu.nl/enums/widgets/view#preview_view"),
            widgetResource = info,
        ),
    )
}

private fun DataSlice.tenantWidget(tenants: List<TenantDescription>): Value.Id {
    val tenantEntities = tenants.map {
        record {
            type(Schema.Thing.id)
            field(Schema.name) { s(it.name) }
            field(Schema.text) { s(it.location.toString()) }
            field(Ontola.href) { id(it.location.toString()) }
        }
    }

    val collection = buildCollection {
        id = Value.Id.Global("/home/widgets/tenants/items")
        title = "Tenants"
        display = CollectionDisplay.Table()

        addColumn(Schema.name)
        addColumn(Schema.text)
        addColumn(Ontola.href)

        addSorting(Schema.name, SortDirection.Desc)

        addAll(tenantEntities)
    }

    return add(
        Widget(
            id = Value.Id.Global("/home/widgets/tenants"),
            order = 1,
            topology = Value.Id.Global("https://ns.ontola.io/libro/topologies/card"),
            widgetSize = 2,
            view = Value.Id.Global("https://argu.nl/enums/widgets/view#preview_view"),
            widgetResource = collection.id,
        ),
    )
}

private fun DataSlice.studioWidget(): Value.Id {
    val entryPoint = Value.Id.Global("/home/widgets/studio/list/entryPoint")

    val action = add(
        Action(
            id = Value.Id.Global("/home/widgets/studio/list/action"),
            name = "Studio",
            text = "Open the studio",
            target = entryPoint,
            actionStatus = Value.Id.Global("http://schema.org/PotentialActionStatus"),
        ),
    )

    add(
        EntryPoint(
            id = entryPoint,
            name = "Open",
            isPartOf = action,
            httpMethod = "GET",
            formTarget = "_blank",
            url = Value.Id.Global("/libro/studio"),
        ),
    )

    return add(
        Widget(
            id = Value.Id.Global("/home/widgets/studio"),
            order = 0,
            topology = Value.Id.Global("https://ns.ontola.io/libro/topologies/grid"),
            widgetSize = 1,
            view = Value.Id.Global("https://argu.nl/enums/widgets/view#preview_view"),
            widgetResource = entryPoint,
        ),
    )
}

private fun DataSlice.healthWidget(checks: List<Check>): Value.Id {
    val collectionBase = Value.Id.Global("/home/widgets/health/checks")

    val checkRecords = checks.mapIndexed { index, check ->
        record(Value.Id.Global("/home/widgets/health/checks/items/$index")) {
            type(Schema.Thing.id)
            field(Schema.name) { s(check.name) }
            field(Libro.result) { s(humanStatus(check.result)) }
            field(Libro.message) { s(if (check.result == CheckResult.Pass) "N/A" else check.message) }
        }
    }

    val collection = buildCollection {
        id = collectionBase
        title = "Health checks"
        display = CollectionDisplay.Table()

        addColumn(Schema.name)
        addColumn(Libro.result)
        addColumn(Libro.message)

        addSorting(Schema.name, SortDirection.Desc)

        addAll(checkRecords)
    }

    return add(
        Widget(
            id = Value.Id.Global("/home/widgets/health"),
            order = 0,
            topology = Value.Id.Global("https://ns.ontola.io/libro/topologies/card"),
            widgetSize = 3,
            view = Value.Id.Global("https://argu.nl/enums/widgets/view#preview_view"),
            widgetResource = collection.id,
        ),
    )
}
