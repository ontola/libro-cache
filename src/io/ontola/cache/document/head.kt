package io.ontola.cache.document

import io.ontola.cache.plugins.Manifest
import kotlinx.html.HEAD
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun HEAD.renderHead(nonce: String, config: PageConfiguration, manifest: Manifest) {
    val pageName = manifest.name

    meta(charset = "utf-8")
    title(pageName)
    link(rel = "manifest", href = "${manifest.scope}/manifest.json")
    meta(name = "website", content = manifest.ontola.websiteIRI.toString())
    manifest.ontola.preconnect?.forEach { link(rel = "preconnect", href = it) }

    // Statics
    config.tileServerUrl?.let { meta(name = "mapboxTileURL", content = it) }
    manifest.ontola.websocketPath?.ifBlank { null }?.let { meta(name = "websocket-path", content = it) }
    config.bugsnagOpts?.let {
        script(type = "application/javascript", src = "https://d2wy8f7a9ursnm.cloudfront.net/v6/bugsnag.min.js") {
            async = true
        }
        meta(name = "bugsnagConfig", content = Json.encodeToString(config.bugsnagOpts))
    }
    link(rel = "stylesheet", type = "text/css", href = config.assets.mainCss) {
        attributes["crossorigin"] = "anonymous"
    }
    link(href = "https://fonts.googleapis.com/css?family=Open+Sans:400,700", rel = "stylesheet")
    link(rel = "stylesheet", href = "https://maxcdn.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css") {
        attributes["crossorigin"] = "anonymous"
    }

    // Web app / styles
    meta(name = "theme", content = manifest.ontola.theme ?: "common")
    meta(name = "themeOpts", content = manifest.ontola.themeOptions ?: "")
    meta(content = pageName) {
        attributes["property"] = "og:title"
    }
    meta(content = "website") {
        attributes["property"] = "og:type"
    }
    config.facebookAppId?.let {
        meta(content = it) {
            attributes["property"] = "fb:app_id"
        }
    }
    meta(name = "mobile-web-app-capable", content = "yes")
    meta(name = "apple-mobile-web-app-capable", content = "yes")
    meta(name = "application-name", content = manifest.shortName)
    meta(name = "apple-mobile-web-app-title", content = manifest.shortName)
    meta(name = "theme-color", content = manifest.themeColor)
    meta(name = "msapplication-navbutton-color", content = manifest.themeColor)
    meta(name = "apple-mobile-web-app-status-bar-style", content = "black-translucent")
    meta(name = "msapplication-starturl", content = manifest.startUrl.toString())
    meta(name = "viewport", content = "width=device-width, shrink-to-fit=no, initial-scale=1, maximum-scale=1.0, user-scalable=yes")
    meta(name = "theme", content = manifest.ontola.theme ?: "common")
    meta(name = "themeOpts", content = manifest.ontola.themeOptions)
    meta(name = "msapplication-TileColor", content = manifest.themeColor)
    meta(name = "msapplication-config", content = "/assets/favicons/browserconfig.xml")

    style {
        this.nonce = nonce
        unsafe {
            +preloaderCss
        }
    }

    manifest.icons?.forEach { icon ->
        when {
            icon.src.contains("favicon") -> {
                link(rel = "icon", type = icon.type, href = icon.src) {
                    attributes["sizes"] = icon.sizes
                }
            }
            icon.src.contains("apple-touch-icon") -> {
                link(rel = "apple-touch-icon", type = icon.type, href = icon.src) {
                    attributes["sizes"] = icon.sizes
                }
            }
            icon.src.contains("mstile") -> {
                meta(name = "msapplication-TileImage", content = icon.src)
            }
        }
    }
}
