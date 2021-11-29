package io.ontola.cache.document

import io.ontola.cache.tenantization.Manifest
import io.ontola.cache.tenantization.TrackerType
import io.ontola.cache.tenantization.Tracking
import kotlinx.html.HEAD
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun HEAD.renderHead(url: String, nonce: String, config: PageConfiguration, manifest: Manifest, seed: String) {
    opening(manifest)

    unsafe {
        raw(renderedMetaTags(url, manifest, seed))
        raw("\n")
    }

    headTracking(nonce, manifest.ontola.tracking)

    // Statics
    services(config, manifest)
    stylesheets(config)

    // Web app / styles
    theming(manifest)
    fbAppId(config)
    webAppConfig(manifest)
    preloader(nonce)
    appIcons(manifest)
}

private fun HEAD.preloader(nonce: String) {
    style {
        this.nonce = nonce
        unsafe {
            +preloaderCss
        }
    }
}

private fun HEAD.appIcons(manifest: Manifest) {
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

private fun HEAD.opening(manifest: Manifest) {
    meta(charset = "utf-8")
    title(manifest.name)
    link(rel = "manifest", href = "${manifest.scope}/manifest.json")
    meta(name = "website", content = manifest.ontola.websiteIRI.toString())
    manifest.ontola.preconnect?.forEach { link(rel = "preconnect", href = it) }
    meta(content = manifest.name) {
        attributes["property"] = "og:title"
    }
    meta(content = "website") {
        attributes["property"] = "og:type"
    }
}

private fun HEAD.services(config: PageConfiguration, manifest: Manifest) {
    config.tileServerUrl?.let { meta(name = "mapboxTileURL", content = it) }
    manifest.ontola.websocketPath?.ifBlank { null }?.let { meta(name = "websocket-path", content = it) }
    config.bugsnagOpts?.let {
        script(type = "application/javascript", src = "https://d2wy8f7a9ursnm.cloudfront.net/v6/bugsnag.min.js") {
            async = true
        }
        meta(name = "bugsnagConfig", content = Json.encodeToString(config.bugsnagOpts))
    }
}

private fun HEAD.stylesheets(config: PageConfiguration) {
    link(rel = "stylesheet", type = "text/css", href = config.assets.es6.mainCss) {
        attributes["crossorigin"] = "anonymous"
    }
    link(href = "https://fonts.googleapis.com/css?family=Open+Sans:400,700", rel = "stylesheet")
    link(rel = "stylesheet", href = "https://maxcdn.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css") {
        attributes["crossorigin"] = "anonymous"
    }
}

private fun HEAD.theming(manifest: Manifest) {
    meta(name = "theme", content = manifest.ontola.theme ?: "common")
    meta(name = "themeOpts", content = manifest.ontola.themeOptions ?: "")
}

private fun HEAD.fbAppId(config: PageConfiguration) {
    config.facebookAppId?.let {
        meta(content = it) {
            attributes["property"] = "fb:app_id"
        }
    }
}

private fun HEAD.webAppConfig(manifest: Manifest) {
    meta(name = "mobile-web-app-capable", content = "yes")
    meta(name = "apple-mobile-web-app-capable", content = "yes")
    meta(name = "application-name", content = manifest.shortName)
    meta(name = "apple-mobile-web-app-title", content = manifest.shortName)
    meta(name = "theme-color", content = manifest.themeColor)
    meta(name = "msapplication-navbutton-color", content = manifest.themeColor)
    meta(name = "apple-mobile-web-app-status-bar-style", content = "black-translucent")
    meta(name = "msapplication-starturl", content = manifest.startUrl)
    meta(name = "viewport", content = "width=device-width, shrink-to-fit=no, initial-scale=1, maximum-scale=1.0, user-scalable=yes")
    meta(name = "theme", content = manifest.ontola.theme ?: "common")
    meta(name = "themeOpts", content = manifest.ontola.themeOptions)
    meta(name = "msapplication-TileColor", content = manifest.themeColor)
    meta(name = "msapplication-config", content = "/assets/favicons/browserconfig.xml")
}

private fun HEAD.headTracking(nonce: String, tracking: List<Tracking>) {
    tracking.forEach {
        when (it.type) {
            TrackerType.GTM -> {
                script {
                    this.nonce = nonce

                    unsafe {
                        raw(
                            """
                            (function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
                            new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
                            j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
                            'https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
                            })(window,document,'script','dataLayer','${it.containerId}');
                            """.trimIndent()
                        )
                    }
                }
            }
            else -> Unit
        }
    }
}
