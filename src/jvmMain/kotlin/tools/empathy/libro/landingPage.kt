package tools.empathy.libro

import io.ktor.client.plugins.ServerResponseException
import io.ktor.server.application.ApplicationCall
import io.ontola.cache.health.BackendCheck
import io.ontola.cache.health.BulkCheck
import io.ontola.cache.health.CheckResult
import io.ontola.cache.health.EnvironmentCheck
import io.ontola.cache.health.HeadRequestCheck
import io.ontola.cache.health.ManifestCheck
import io.ontola.cache.health.RedisCheck
import io.ontola.cache.health.humanStatus
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.tenantization.TenantsResponse
import io.ontola.cache.tenantization.getTenants
import io.ontola.empathy.web.DataSlice
import io.ontola.empathy.web.Value
import io.ontola.empathy.web.dataSlice
import io.ontola.empathy.web.field
import io.ontola.empathy.web.id
import io.ontola.empathy.web.record
import io.ontola.empathy.web.s
import io.ontola.empathy.web.type
import tools.empathy.model.Action
import tools.empathy.model.CollectionDisplay
import tools.empathy.model.EntryPoint
import tools.empathy.model.Seq
import tools.empathy.model.SortDirection
import tools.empathy.model.WebPage
import tools.empathy.model.WebSite
import tools.empathy.model.Widget
import tools.empathy.model.add
import tools.empathy.model.addAll
import tools.empathy.model.addColumn
import tools.empathy.model.addSorting
import tools.empathy.model.buildCollection
import tools.empathy.vocabularies.Libro
import tools.empathy.vocabularies.Ontola
import tools.empathy.vocabularies.Schema

suspend fun ApplicationCall.landingPage(): DataSlice = dataSlice {
    val tenants = try {
        this@landingPage.getTenants()
    } catch (e: ServerResponseException) {
        null
    }

    val defaultServicePort = application.cacheConfig.services.config("base").propertyOrNull("defaultServicePort")
    val homePage = add(
        WebPage(
            id = Value.Id.Global("https://localhost/home"),
            name = "Homepage",
            text = "Welcome to Libro",
            widgets = add(
                Seq(
                    Value.Id.Global("https://localhost/home/widgets"),
                    listOf(
                        if (tenants == null) noBackendWidget(defaultServicePort?.getString()) else tenantWidget(tenants),
                        studioWidget(),
                        healthWidget(this@landingPage),
                    )
                )
            ),
        )
    )

    add(
        WebSite(
            id = Value.Id.Global("https://localhost"),
            name = "Libro",
            text = "Backend ${if (tenants == null) "not " else ""}found",
            homepage = homePage,
        )
    )
}

private fun DataSlice.studioWidget(): Value.Id {
    val entryPoint = Value.Id.Global("https://localhost/home/widgets/studio/list/entryPoint")

    val action = add(
        Action(
            id = Value.Id.Global("https://localhost/home/widgets/studio/list/action"),
            name = "Studio",
            text = "Open the studio",
            target = entryPoint,
            actionStatus = Value.Id.Global("http://schema.org/PotentialActionStatus"),
        )
    )

    add(
        EntryPoint(
            id = entryPoint,
            name = "Open",
            isPartOf = action,
            httpMethod = "GET",
            url = Value.Id.Global("/d/studio"),
        )
    )

    return add(
        Widget(
            id = Value.Id.Global("https://localhost/home/widgets/studio"),
            order = 0,
            topology = Value.Id.Global("https://ns.ontola.io/libro/topologies/grid"),
            widgetSize = 1,
            view = Value.Id.Global("https://argu.nl/enums/widgets/view#preview_view"),
            widgetResource = entryPoint,
        )
    )
}

private suspend fun DataSlice.healthWidget(call: ApplicationCall): Value.Id {
    val checks = listOf(
        BackendCheck(),
        EnvironmentCheck(),
        RedisCheck(),
        HeadRequestCheck(),
        ManifestCheck(),
        BulkCheck(),
    )

    checks.forEach { it.run(call) }

    val collectionBase = Value.Id.Global("https://localhost/home/widgets/health/checks")

    val checkRecords = checks.mapIndexed { index, check ->
        record(Value.Id.Global("https://localhost/home/widgets/health/checks/items/$index")) {
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
            id = Value.Id.Global("https://localhost/home/widgets/health"),
            order = 0,
            topology = Value.Id.Global("https://ns.ontola.io/libro/topologies/card"),
            widgetSize = 3,
            view = Value.Id.Global("https://argu.nl/enums/widgets/view#preview_view"),
            widgetResource = collection.id,
        )
    )
}

private fun DataSlice.noBackendWidget(defaultServicePort: String?): Value.Id {
    val info = record {
        type(Schema.CreativeWork)
        field(Schema.name) { s("No backend found") }
        field(Schema.text) {
            val portHelper = if (defaultServicePort == null) "" else " (`$defaultServicePort`)"

            s("""
                No data service detected. Check whether the `DEFAULT_SERVICE_PORT`$portHelper is correctly set and the server is running on that port.
                
                See the [linked_rails framework](https://github.com/ontola/linked_rails) to build a Libro compatible service. 
            """.trimIndent())
        }
    }

    return add(
        Widget(
            id = Value.Id.Global("https://localhost/home/widgets/tenants"),
            order = 1,
            topology = Value.Id.Global("https://ns.ontola.io/libro/topologies/container"),
            widgetSize = 2,
            view = Value.Id.Global("https://argu.nl/enums/widgets/view#preview_view"),
            widgetResource = info,
        )
    )
}

private fun DataSlice.tenantWidget(tenants: TenantsResponse): Value.Id {
    val tenantEntities = tenants.sites.map {
        record {
            type(Schema.Thing.id)
            field(Schema.name) { s(it.name) }
            field(Schema.text) { s(it.location.toString()) }
            field(Ontola.href) { id(it.location.toString()) }
        }
    }

    val collection = buildCollection {
        id = Value.Id.Global("https://localhost/home/widgets/tenants/items")
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
            id = Value.Id.Global("https://localhost/home/widgets/tenants"),
            order = 1,
            topology = Value.Id.Global("https://ns.ontola.io/libro/topologies/card"),
            widgetSize = 2,
            view = Value.Id.Global("https://argu.nl/enums/widgets/view#preview_view"),
            widgetResource = collection.id,
        )
    )
}
