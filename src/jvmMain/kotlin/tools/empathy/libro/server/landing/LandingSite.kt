package tools.empathy.libro.server.landing

import io.ktor.client.plugins.ResponseException
import io.ktor.server.application.ApplicationCall
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.health.BackendCheck
import tools.empathy.libro.server.health.BulkCheck
import tools.empathy.libro.server.health.EnvironmentCheck
import tools.empathy.libro.server.health.HeadRequestCheck
import tools.empathy.libro.server.health.RedisCheck
import tools.empathy.libro.server.plugins.Versions
import tools.empathy.libro.server.tenantization.getApiVersion
import tools.empathy.libro.server.tenantization.getTenants
import tools.empathy.libro.server.util.LibroHttpHeaders
import tools.empathy.model.Seq
import tools.empathy.model.WebSite
import tools.empathy.model.add
import tools.empathy.model.menu.MenuItem
import tools.empathy.model.menu.add
import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Value
import tools.empathy.serialization.dataSlice

suspend fun ApplicationCall.landingSite(): DataSlice = dataSlice {
    val defaultServicePort = application
        .libroConfig
        .services
        .config("base")
        .propertyOrNull("defaultServicePort")
        ?.getString()
    val tenants = try {
        this@landingSite.getTenants()
    } catch (e: ResponseException) {
        null
    }

    val versions = VersionSet(
        api = getApiVersion(),
        server = Versions.ServerVersion,
        client = request.headers[LibroHttpHeaders.XClientVersion],
    )

    val checks = listOf(
        BackendCheck(),
        EnvironmentCheck(),
        RedisCheck(),
        HeadRequestCheck(),
        BulkCheck(),
    )

    checks.forEach { it.run(this@landingSite) }

    val homePage = homePage(
        tenants,
        defaultServicePort,
        versions,
        checks,
        application.libroConfig.isDev,
    )
    val modulesPage = modulesPage()

    val navigationsMenu = add(
        MenuItem(
            id = Value.Id.Global("/menus/navigations"),
            name = "Navigations",
            menuItems = add(
                Seq(
                    Value.Id.Global("/menus/navigations/menu_items"),
                    listOf(
                        add(
                            MenuItem(
                                id = Value.Id.Local(),
                                name = "Libro",
                                encodingFormat = "image/svg+xml",
                                image = Value.Id.Global("/f_assets/images/libro-logo-t-4.svg"),
                                customImage = Value.Id.Global("/f_assets/images/libro-logo-t-4.svg"),
                                isPartOf = Value.Id.Global("/"),
                                edge = Value.Id.Global("/"),
                                href = Value.Id.Global("/"),
                                targetType = Value.Id.Global("https://argu.nl/enums/custom_menu_items/target_type#edge"),
                                order = 0,
                            )
                        ),
                        add(
                            MenuItem(
                                id = Value.Id.Local(),
                                name = "Home",
                                isPartOf = Value.Id.Global("/home"),
                                edge = homePage,
                                href = homePage,
                                targetType = Value.Id.Global("https://argu.nl/enums/custom_menu_items/target_type#edge"),
                                order = 1,
                            )
                        ),
                        add(
                            MenuItem(
                                id = Value.Id.Local(),
                                name = "Modules",
                                isPartOf = Value.Id.Global("/"),
                                edge = modulesPage,
                                href = modulesPage,
                                targetType = Value.Id.Global("https://argu.nl/enums/custom_menu_items/target_type#edge"),
                                order = 2,
                            )
                        ),
                    )
                )
            )
        )
    )

    add(
        WebSite(
            id = Value.Id.Global("https://localhost/"),
            name = "Libro",
            text = "Backend ${if (tenants == null) "not " else ""}found",
            homepage = homePage,
            navigationsMenu = navigationsMenu,
        ),
    )
}
