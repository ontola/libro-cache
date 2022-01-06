package io.ontola.cache.plugins

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.plugin
import io.ktor.server.request.host
import io.ktor.server.response.header
import io.ktor.util.AttributeKey
import io.ontola.apex.webmanifest.Manifest
import io.ontola.apex.webmanifest.TrackerType
import io.ontola.cache.plugins.CSPSettings.toCSPHeader
import io.ontola.cache.tenantization.blacklisted
import io.ontola.cache.tenantization.tenant
import java.util.UUID

class CSPContext(
    val development: Boolean,
    val nonce: String,
    val host: String,
    val manifest: Manifest?,
)

class CSPEntry(val constant: String? = null, val dynamic: ((ctx: CSPContext) -> String?)? = null)

object CSPValue {
    const val Self = "'self'"
    const val Blob = "blob:"
    const val Data = "data:"
    const val UnsafeEval = "'unsafe-eval'"
    const val UnsafeInline = "'unsafe-inline'"

    fun nonce(nonce: String) = "nonce-$nonce"
}

object CSPSettings {
    private val oneOffs = listOf(
        "upgrade-insecure-requests"
    )

    private val defaultSrc = listOf(
        CSPEntry(CSPValue.Self)
    )

    private val childSrc = listOf(
        CSPEntry("https://youtube.com"),
        CSPEntry("https://www.youtube.com"),
    )
    private val connectSrc = listOf(
        CSPEntry(CSPValue.Self),
        CSPEntry("https://api.notubiz.nl"),
        CSPEntry("https://api.openraadsinformatie.nl"),
        CSPEntry("https://www.facebook.com"),
        CSPEntry("https://analytics.argu.co"),
        CSPEntry("https://argu-logos.s3.eu-central-1.amazonaws.com"),
        CSPEntry { ctx -> "ws://${ctx.host}" },
        CSPEntry { ctx -> if (!ctx.development) "https://notify.bugsnag.com" else null },
        CSPEntry { ctx -> if (!ctx.development) "https://sessions.bugsnag.com" else null },
    )

    private val fontSrc = listOf(
        CSPEntry(CSPValue.Self),
        CSPEntry("https://maxcdn.bootstrapcdn.com"),
        CSPEntry("https://fonts.gstatic.com"),
    )
    private val frameSrc = listOf(
        CSPEntry("https://youtube.com"),
        CSPEntry("https://www.youtube.com"),
        CSPEntry("https://*.typeform.com/"),
        CSPEntry("https://webforms.pipedrive.com"),
    )
    private val imgSrc = listOf(
        CSPEntry(CSPValue.Self),
        CSPEntry(CSPValue.Blob),
        CSPEntry(CSPValue.Data),
        CSPEntry("*"),
    )
    private val objectSrc = listOf(
        CSPEntry("'none'"),
    )
    private val sandbox = listOf(
        CSPEntry("allow-downloads"),
        CSPEntry("allow-forms"),
        CSPEntry("allow-modals"),
        CSPEntry("allow-popups"),
        CSPEntry("allow-popups-to-escape-sandbox"),
        CSPEntry("allow-presentation"),
        CSPEntry("allow-same-origin"),
        CSPEntry("allow-scripts"),
    )
    private val scriptSrc = listOf(
        CSPEntry(CSPValue.Self),
        CSPEntry(CSPValue.UnsafeEval),
        CSPEntry { ctx -> CSPValue.nonce(ctx.nonce) },
        CSPEntry("https://cdn.polyfill.io"),
        // Bugsnag CDN
        CSPEntry("https://d2wy8f7a9ursnm.cloudfront.net"),
        CSPEntry("https://storage.googleapis.com"),
        CSPEntry("https://www.googletagmanager.com"),
        CSPEntry("https://webforms.pipedrive.com/f/loader"),
        CSPEntry("https://cdn.eu-central-1.pipedriveassets.com/leadbooster-chat/assets/web-forms/loader.min.js"),
        CSPEntry("https://browser-update.org"),
        CSPEntry("https://argu-logos.s3.eu-central-1.amazonaws.com"),
        CSPEntry("https://cdnjs.cloudflare.com"),
        CSPEntry { ctx ->
            ctx
                .manifest
                ?.ontola
                ?.tracking
                ?.ifEmpty { return@CSPEntry null }
                ?.filter { it.type == TrackerType.Matomo || it.type == TrackerType.PiwikPro }
                ?.joinToString(" ") { "https://${it.host}" }
        },
        CSPEntry { ctx -> if (ctx.development) CSPValue.UnsafeInline else null },
        CSPEntry { ctx -> if (ctx.development) CSPValue.UnsafeEval else null },
        CSPEntry { ctx -> if (ctx.development) CSPValue.Blob else null },
    )

    private val styleSrc = listOf(
        CSPEntry(CSPValue.Self),
        // Due to using inline css with background-image url()
        CSPEntry(CSPValue.UnsafeInline),
        CSPEntry("maxcdn.bootstrapcdn.com"),
        CSPEntry("fonts.googleapis.com"),
        CSPEntry { ctx -> if (ctx.development) CSPValue.Blob else null },
    )
    private val workerSrc = listOf(
        CSPEntry(CSPValue.Self),
        CSPEntry { ctx -> if (ctx.development) CSPValue.Blob else null },
    )
    private val mediaSrc = listOf(
        CSPEntry(CSPValue.Self),
    )

    fun CSPContext.toCSPHeader(): String = buildString {
        fun appendList(name: String, entries: List<CSPEntry>) {
            val content = entries
                .mapNotNull { it.constant ?: it.dynamic!!(this@toCSPHeader) }
                .joinToString(" ")

            if (name.isBlank() || content.isBlank()) {
                return
            }

            append(" ")
            append(name)
            append(" ")
            append(content)
            append(";")
        }

        oneOffs
            .joinToString(" ")
            .ifBlank { null }
            ?.let { append("$it;") }
        appendList("default-src", defaultSrc)
        appendList("child-src", childSrc)
        appendList("connect-src", connectSrc)
        appendList("font-src", fontSrc)
        appendList("frame-src", frameSrc)
        appendList("img-src", imgSrc)
        appendList("object-src", objectSrc)
        appendList("sandbox", sandbox)
        appendList("script-src", scriptSrc)
        appendList("style-src", styleSrc)
        appendList("worker-src", workerSrc)
        appendList("media-src", mediaSrc)
    }
}

val CSP = createApplicationPlugin("CSP") {
    val isDevelopment = application.developmentMode

    onCall { call ->
        val nonce = UUID.randomUUID().toString()
        call.attributes.put(CSPKey, nonce)

        val ctx = CSPContext(
            isDevelopment,
            nonce,
            call.request.host(),
            if (call.blacklisted) null else call.tenant.manifest,
        )

        call.response.header("Content-Security-Policy", ctx.toCSPHeader())
    }
}

private val CSPKey = AttributeKey<String>("CSPKey")

internal val ApplicationCall.nonce: String
    get() = attributes.getOrNull(CSPKey) ?: reportMissingNonce()

private fun ApplicationCall.reportMissingNonce(): Nothing {
    application.plugin(CacheSession) // ensure the feature is installed
    throw NonceNotYetConfiguredException()
}

class NonceNotYetConfiguredException :
    IllegalStateException("Nonce is not yet ready: you are asking it to early before the CSP feature.")
