package io.ontola.cache.document

import io.ontola.apex.webmanifest.Manifest
import io.ontola.cache.assets.AssetsManifests
import io.ontola.color.Color
import io.ontola.color.isLight
import io.ontola.empathy.web.DataSlice
import kotlinx.html.BODY
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.id
import kotlinx.html.lang
import kotlinx.html.noScript
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.unsafe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.text.translate.AggregateTranslator
import org.apache.commons.text.translate.LookupTranslator
import kotlin.collections.set

@Serializable
data class BugsnagOpts(
    val apiKey: String,
    val appVersion: String?,
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
            +"""
            if ('serviceWorker' in navigator) {
              window.addEventListener('load', function() {
                navigator.serviceWorker.register('/f_assets/sw.js', { scope: '${manifest.serviceworker.scope}/' });
              });
              window.addEventListener('load', function() {
                 navigator.serviceWorker.register('/f_assets/sw.js', { scope: '${manifest.serviceworker.scope}' });
              });
            }
            """.trimIndent()
        }
    }
}

fun BODY.preloadBlock(nonce: String, config: PageConfiguration, manifest: Manifest) {
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
        this.nonce = nonce
        unsafe {
            +"document.body.className = (document.body.className || '') + ' Body--show-preloader';"
        }
    }
}

fun luminanceBased(
    check: String,
    truthy: String,
    falsy: String,
): String = if (Color.fromCss(check).isLight()) {
    truthy
} else {
    falsy
}

fun BODY.themeBlock(manifest: Manifest) {
    style {
        attributes["id"] = "theme-config"

        unsafe {
            raw(
                """
                :root {
                  --accent-background-color:${manifest.ontola.primaryColor};
                  --accent-color:${luminanceBased(manifest.ontola.primaryColor, "#222222", "#FFFFFF")};
                  --navbar-background:${navbarBackground(manifest)};
                }
                """.trimIndent()
            )
        }
    }
}

private val JsonInHtmlEscaper = AggregateTranslator(
    LookupTranslator(
        mapOf(
            "<" to "&lt;",
            ">" to "&gt;",
        )
    ),
)

fun BODY.seedBlock(nonce: String, data: DataSlice) {
    script(type = "application/link-rehydrate+json") {
        this.nonce = nonce
        attributes["id"] = "seed"
        unsafe {
            +JsonInHtmlEscaper.translate(Json.encodeToString(data))
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

fun HTML.indexPage(ctx: PageRenderContext) {
    val url = ctx.uri.toString()
    val nonce = ctx.nonce
    val config = ctx.configuration
    val manifest = ctx.manifest
    val data = ctx.data ?: emptyMap()

    lang = ctx.lang

    head {
        renderHead(url, nonce, ctx.csrfToken, config, manifest, ctx.lang, data)
    }
    body {
        attributes["style"] = "margin: 0;"

        themeBlock(manifest)
        preloadBlock(nonce, config, manifest)
        serviceWorkerBlock(nonce, manifest)
        seedBlock(nonce, data)
        assetsBlock(nonce, config)
//            deferredBodyStyles(nonceStr)
        manifestBlock(nonce, manifest, ctx.serializer)
//            browserUpdateBlock()
        bodyTracking(nonce, manifest.ontola.tracking, ctx.isUser)
    }
}
