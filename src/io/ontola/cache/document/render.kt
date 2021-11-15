package io.ontola.cache.document

import io.ontola.cache.plugins.Manifest
import kotlinx.html.BODY
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.noScript
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.unsafe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ResourcesManifest(
    @SerialName("main.css")
    val mainCss: String = "/dist/main.bundle.css",
    @SerialName("main.js")
    val mainJs: String = "/dist/main.bundle.js",
    @SerialName("main.js.map")
    val mainJsMap: String? = "/dist/main.js.map",
    @SerialName("Forms.css")
    val FormsValCss: String? = "/dist/Forms.css",
    @SerialName("Forms.js")
    val FormsValJs: String? = "/dist/Forms.js",
    @SerialName("Forms.js.map")
    val FormsJsMap: String? = "/dist/Forms.js.map",
    @SerialName("MapView.js")
    val MapViewValJs: String? = "/dist/MapView.js",
    @SerialName("MapView.js.map")
    val MapViewJsMap: String? = "/dist/MapView.js.map",
    @SerialName("vendors~Forms.js")
    val vendorsFormsJs: String? = "/dist/vendors~Forms.js",
    @SerialName("vendors~Forms.js.map")
    val vendorsFormsJsMap: String? = "/dist/vendors~Forms.js.map",
    @SerialName("vendors~MapView.css")
    val vendorsMapViewCss: String? = "/dist/vendors~MapView.css",
    @SerialName("vendors~MapView.js")
    val vendorsMapViewJs: String? = "/dist/vendors~MapView.js",
    @SerialName("vendors~MapView.js.map")
    val vendorsMapViewJsMap: String? = "/dist/vendors~MapView.js.map",
    @SerialName("vendors~Typeform.js")
    val vendorsTypeformJs: String? = "/dist/vendors~Typeform.js",
    @SerialName("vendors~Typeform.js.map")
    val vendorsTypeformJsMap: String? = "/dist/vendors~Typeform.js.map",
    @SerialName("vendors~main.js")
    val vendorsMainJs: String = "/dist/vendors~main.js",
    @SerialName("vendors~main.js.map")
    val vendorsMainJsMap: String? = "/dist/vendors~main.js.map",
)

@Serializable
data class BugsnagOpts(
    val apiKey: String,
    val appVersion: String,
    val releaseStage: String,
)

@Serializable
data class PageConfiguration(
    val appElement: String,
    val assets: ResourcesManifest = ResourcesManifest(),
    val env: String? = null,
    val bugsnagOpts: BugsnagOpts? = null,
    val facebookAppId: String? = null,
    val tileServerUrl: String? = null,
) {
    companion object {
        inline val PageConfiguration.envKind get() = this.env
        inline val PageConfiguration.isDev get() = envKind == "dev"
        inline val PageConfiguration.isProd get() = envKind != "dev"
    }
}

fun navbarBackground(manifest: Manifest) = when (manifest.ontola.headerBackground) {
    "white" -> "#FFFFFF"
    "secondary" -> manifest.ontola.secondaryColor
    "primary" -> manifest.ontola.primaryColor
    else -> manifest.ontola.headerBackground
}

fun BODY.serviceWorkerBlock(nonce: String, manifest: Manifest) {
    script {
        this.nonce = nonce
        unsafe {
            """
            if ('serviceWorker' in navigator) {
              window.addEventListener('load', function() {
                navigator.serviceWorker.register('${manifest.serviceworker.src}', { scope: '${manifest.serviceworker.scope}/' });
              });
              window.addEventListener('load', function() {
                 navigator.serviceWorker.register('${manifest.serviceworker.src}', { scope: '${manifest.serviceworker.scope}' });
              });
            }
            """.trimIndent()
        }
    }
}

fun BODY.preloadBlock(config: PageConfiguration, manifest: Manifest) {
    div {
        id = "preloader"
        classes = setOf("preloader")

        div {
            classes = setOf("spinner")
            listOf("rect1", "rect2", "rect3", "rect4", "rect5").forEach {
                div {
                    classes = setOf(it)
                }
            }
        }
    }
    div {
        id = "navbar-preview"
        style {
//          +"background: ${manifest.navbarBackground}; color: ${navbarColor(ctx)}; height: 3.2rem; z-index: -1;"
            +"height: 3.2rem; z-index: -1;"
        }
    }
    div {
        id = config.appElement
        classes = setOf(manifest.ontola.theme ?: "common", "preloader-fixed")
    }
    noScript {
        h1 { +"Deze website heeft javascript nodig om te werken" }
        p { +"Javascript staat momenteel uitgeschakeld, probeer een andere browser of in prive modus." }
    }
    script {
        unsafe {
            +"document.body.className = (document.body.className || '') + ' Body--show-preloader';"
        }
    }
}

fun luminanceBased(check: String, truthy: String, falsy: String): String {
//    val test = Color(check)
//    val R = test.red
//
//    val L = 0.2126 * R + 0.7152 * G + 0.0722 * B

    return falsy
}

fun BODY.themeBlock(manifest: Manifest) {
    style {
        attributes["id"] = "theme-config"

        val checkLuminance = true // checkLuminance(ctx.manifest.ontola.primary_color)
        val background = "#" // navbarBackground(ctx)
        val navbarBackground =
            unsafe {
                raw(
                    """
                    :root {
                      --accent-background-color: ${manifest.ontola.primaryColor};
                      --accent-color: ${luminanceBased(manifest.ontola.primaryColor, "#222222", "#FFFFFF")};
                      --navbar-background: ${navbarBackground(manifest)};
                    }
                    """.trimIndent()
                )
            }
    }
}

fun BODY.seedBlock(nonce: String, seed: String) {
    script(type = "application/hex+x-ndjson") {
        this.nonce = nonce
        attributes["id"] = "seed"
        unsafe {
            +seed
        }
    }
    script(type = "application/javascript") {
        this.nonce = nonce
        unsafe {
            +"var seed = document.getElementById('seed');"
            +"window.INITIAL__DATA = seed ? seed.textContent : '';"
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun BODY.manifestBlock(nonce: String, manifest: Manifest, serializer: Json) {
    script(type = "application/javascript") {
        this.nonce = nonce
        unsafe {
            raw("window.WEBSITE_MANIFEST = JSON.parse('${serializer.encodeToString(manifest)}');")
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun BODY.assetsBlock(nonce: String, config: PageConfiguration) {
    script(type = "module") {
        async = true
        this.nonce = nonce
        attributes["crossorigin"] = "anonymous"
        src = config.assets.mainJs
    }
    script(type = "module") {
        async = true
        this.nonce = nonce
        attributes["crossorigin"] = "anonymous"
        src = config.assets.vendorsMainJs
    }

    script(type = "application/javascript") {
        async = true
        this.nonce = nonce
        attributes["nomodule"] = "true"
        attributes["crossorigin"] = "anonymous"
        src = config.assets.mainJs
    }
    script(type = "application/javascript") {
        async = true
        this.nonce = nonce
        attributes["nomodule"] = "true"
        attributes["crossorigin"] = "anonymous"
        src = config.assets.vendorsMainJs
    }
}

fun HTML.indexPage(
    config: PageConfiguration,
    manifest: Manifest,
    seed: String,
    serializer: Json,
) {
    val nonce = ""

    head {
        renderHead(nonce, config, manifest)
    }
    body {
        attributes["style"] = "margin: 0;"

        themeBlock(manifest)
        preloadBlock(config, manifest)
        serviceWorkerBlock(nonce, manifest)
        seedBlock(nonce, seed)
        assetsBlock(nonce, config)
//            deferredBodyStyles(nonceStr)
        manifestBlock(nonce, manifest, serializer)
//            browserUpdateBlock()
//            bodyTracking()
    }
}
