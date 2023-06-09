package tools.empathy.libro.server.document

import io.ktor.http.Url
import io.ktor.http.fullPath
import kotlinx.html.HEAD
import kotlinx.html.itemProp
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.noScript
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tools.empathy.libro.metadata.getMetaTags
import tools.empathy.libro.metadata.metaDataFromData
import tools.empathy.libro.webmanifest.Manifest
import tools.empathy.libro.webmanifest.TrackerType
import tools.empathy.libro.webmanifest.Tracking
import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Value
import tools.empathy.serialization.canonical
import tools.empathy.serialization.translations
import tools.empathy.url.asHrefString
import tools.empathy.url.origin
import tools.empathy.url.rebase
import tools.empathy.vocabularies.ActivityStreams.rel

fun HEAD.renderHead(
    url: Url,
    nonce: String,
    csrfToken: String,
    config: PageConfiguration,
    manifest: Manifest,
    lang: String,
    data: DataSlice,
) {
    opening(manifest)

    contentMetaTags(url, manifest, data, lang)
    alternates(url, data)
    meta {
        name = "csrf-token"
        content = csrfToken
    }

    headTracking(nonce, manifest.ontola.tracking)

    // Statics
    services(config, manifest)
    stylesheets(config, nonce)

    // Web app / styles
    theming(manifest)
    fbAppId(config)
    webAppConfig(manifest)
    preloader(nonce)
    appIcons(manifest)
}

/**
 * Adds [alternate](https://developers.google.com/search/docs/advanced/crawling/localized-versions#html) links when
 * the [data] contains multiple languages of the same record.
 *
 * Currently only used for the Studio.
 */
private fun HEAD.alternates(
    url: Url,
    data: DataSlice,
) {
    data[url.toString()]
        ?.let { it.canonical()?.first() }
        ?.let { data[it.value] }
        ?.translations()
        ?.filterIsInstance<Value.LangString>()
        ?.forEach {
            link(rel = "alternative", href = it.value) {
                hrefLang = it.lang
            }
        }
}

/**
 * Calculates and injects relevant HTML social tags into the document.
 */
private fun HEAD.contentMetaTags(
    url: Url,
    manifest: Manifest,
    data: DataSlice,
    lang: String,
) {
    val metaData = metaDataFromData(url, manifest, data, lang)
    val tags = getMetaTags(metaData)

    for (tag in tags) {
        when (tag.type) {
            "title" -> title { +tag.children!! }
            "link" -> link(href = tag.href.toString(), rel = tag.rel) {
                tag.itemProp?.let { itemProp = it }
            }
            "meta" -> meta(name = tag.name, content = tag.content) {
                tag.property?.let {
                    attributes["property"] = it
                }
            }
        }
    }
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
        if (icon.purpose?.contains("favicon") == true)
            link(rel = "icon", href = icon.src, type = icon.type) {
                attributes["sizes"] = icon.sizes
            }

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
    link(rel = "manifest", href = manifest.ontola.websiteIRI.rebase("manifest.json").fullPath)
    meta(name = "website", content = manifest.ontola.websiteIRI.asHrefString)
    manifest.ontola.preconnect?.forEach { link(rel = "preconnect", href = it) }
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

private fun HEAD.stylesheets(config: PageConfiguration, nonce: String) {
    val openSans = "https://fonts.googleapis.com/css?family=Open+Sans:400,700&display=swap"
    val fontAwesome = "https://maxcdn.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css"
    link(rel = "preconnect", href = Url(openSans).origin())

    if (!config.bundles.es6.mainCss.contains("null")) {
        link(rel = "stylesheet", type = "text/css", href = config.bundles.es6.mainCss) {
            attributes["crossorigin"] = "anonymous"
        }
    }
    link(rel = "stylesheet", type = "text/css", href = openSans)
    link(rel = "preload", href = fontAwesome) {
        attributes["as"] = "style"
        attributes["crossorigin"] = "anonymous"
    }

    script {
        this.nonce = nonce
        unsafe {
            raw(
                """
                    var elements = Array.from(document.querySelectorAll("[as='style']"));
                    elements.forEach((e) => e.addEventListener("load", (e) => e.target.rel = "stylesheet"));
                    window.setTimeout(() => {
                      elements.map((e) => e.rel = "stylesheet");
                    }, 1000);
                """.trimIndent()
            )
        }
    }

    noScript {
        unsafe {
            raw(
                """
                    <link href="$fontAwesome" rel="stylesheet">
                """.trimIndent()
            )
        }
        link(rel = "stylesheet", href = fontAwesome)
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
    meta(
        name = "viewport",
        content = "width=device-width, shrink-to-fit=no, initial-scale=1, maximum-scale=1.0, user-scalable=yes"
    )
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
