package io.ontola.cache.document

import io.ktor.application.ApplicationCall
import io.ontola.cache.plugins.cacheConfig
import io.ontola.cache.plugins.nonce
import io.ontola.cache.plugins.sessionManager
import io.ontola.cache.tenantization.Manifest
import io.ontola.cache.util.requestUri
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
import kotlin.collections.set

@Serializable
data class AssetsManifests(
    val es5: ResourcesManifest,
    val es6: ResourcesManifest,
)

@Serializable
data class ResourcesManifest(
    val publicFolder: String? = null,
    val defaultBundle: String? = null,
    @SerialName("./sw.js")
    val swJs: String = "/$publicFolder/sw.js",
    @SerialName("main.css")
    val mainCss: String = "/$publicFolder/$defaultBundle.bundle.css",
    @SerialName("main.js")
    val mainJs: String = "/$publicFolder/$defaultBundle.bundle.js",
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
    val assets: AssetsManifests,
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
        src = config.assets.es6.mainJs
    }
    script(type = "application/javascript") {
        async = true
        this.nonce = nonce
        attributes["nomodule"] = "true"
        attributes["crossorigin"] = "anonymous"
        src = config.assets.es5.mainJs
    }
}

fun HTML.indexPage(
    call: ApplicationCall,
    config: PageConfiguration,
    manifest: Manifest,
    seed: String,
) {
    val url = call.requestUri().toString()
    val nonce = call.nonce
    val serializer = call.application.cacheConfig.serializer
    val isUser = call.sessionManager.isUser

    head {
        renderHead(url, nonce, config, manifest, seed)
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
        bodyTracking(nonce, manifest.ontola.tracking, isUser)
    }
}
