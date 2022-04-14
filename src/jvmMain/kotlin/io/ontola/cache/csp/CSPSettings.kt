package io.ontola.cache.csp

import io.ontola.apex.webmanifest.TrackerType

private fun TrackerType.canCallUA(): Boolean = this == TrackerType.GUA || this == TrackerType.GTM

object CSPSettings {
    private val oneOffs = listOf(
        CSPDirectives.UpgradeInsecureRequests,
    )

    private val defaultSrc = listOf(
        CSPEntry(CSPValue.Self),
    )

    private val baseUri = listOf(
        CSPEntry(CSPValue.Self),
    )

    private val reportUri = listOf(
        CSPEntry(cspReportEndpointPath),
    )

    private val formAction = listOf(
        CSPEntry(CSPValue.Self),
    )

    private val manifestSrc = listOf(
        CSPEntry(CSPValue.Self),
        CSPEntry(CSPValue.ReportSample),
        CSPEntry(CSPValue.ReportSample),
    )

    private val childSrc = listOf(
        CSPEntry("https://youtube.com"),
        CSPEntry("https://www.youtube.com"),
        CSPEntry(CSPValue.ReportSample),
    )

    private val connectSrc = listOf(
        CSPEntry(CSPValue.Self),
        CSPEntry("https://api.notubiz.nl"),
        CSPEntry("https://api.openraadsinformatie.nl"),
        CSPEntry("https://www.facebook.com"),
        CSPEntry("https://analytics.argu.co"),
        CSPEntry("https://argu-logos.s3.eu-central-1.amazonaws.com"),
        CSPEntry("https://dptr8y9slmfgv.cloudfront.net"),
        CSPEntry("https://maxcdn.bootstrapcdn.com"),
        CSPEntry("https://fonts.gstatic.com"),
        CSPEntry { ctx -> "ws://${ctx.host}" },
        CSPEntry { ctx -> "wss://${ctx.host}" },
        CSPEntry { ctx -> if (ctx.host == "local.rdf.studio") "*" else null },
        CSPEntry { ctx -> if (!ctx.development) "https://notify.bugsnag.com" else null },
        CSPEntry { ctx -> if (!ctx.development) "https://sessions.bugsnag.com" else null },
        CSPEntry { ctx -> if (ctx.manifest?.ontola?.tracking?.any { it.type.canCallUA() } == true) "https://www.google-analytics.com" else null },
    )

    private val fontSrc = listOf(
        CSPEntry(CSPValue.Self),
        CSPEntry("https://maxcdn.bootstrapcdn.com"),
        CSPEntry("https://fonts.gstatic.com"),
        CSPEntry("https://cdn.jsdelivr.net/npm/monaco-editor@0.25.2/"),
    )

    private val frameSrc = listOf(
        CSPEntry("https://youtube.com"),
        CSPEntry("https://www.youtube.com"),
        CSPEntry("https://*.typeform.com/"),
        CSPEntry("https://webforms.pipedrive.com"),
        CSPEntry(CSPValue.ReportSample),
    )

    private val frameAncestors = listOf(
        CSPEntry(CSPValue.None),
    )

    private val imgSrc = listOf(
        CSPEntry(CSPValue.Self),
        CSPEntry(CSPValue.Blob),
        CSPEntry(CSPValue.Data),
        CSPEntry("*"),
        CSPEntry("https://dptr8y9slmfgv.cloudfront.net"),
        CSPEntry { ctx -> if (ctx.manifest?.ontola?.tracking?.any { it.type.canCallUA() } == true) "https://www.google-analytics.com" else null },
        CSPEntry(CSPValue.ReportSample),
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
        CSPEntry("https://cdn.jsdelivr.net/npm/monaco-editor@0.25.2/"),
        CSPEntry { ctx ->
            ctx
                .manifest
                ?.ontola
                ?.tracking
                ?.ifEmpty { return@CSPEntry null }
                ?.filter { it.host != null && (it.type == TrackerType.Matomo || it.type == TrackerType.PiwikPro) }
                ?.joinToString(" ") { "https://${it.host}" }
        },
        CSPEntry { ctx -> if (ctx.manifest?.ontola?.tracking?.any { it.type.canCallUA() } == true) "https://www.google-analytics.com" else null },
        CSPEntry { ctx -> if (ctx.manifest?.ontola?.tracking?.any { it.type.canCallUA() } == true) "https://ssl.google-analytics.com" else null },
        CSPEntry { ctx -> if (ctx.development) CSPValue.UnsafeInline else null },
        CSPEntry { ctx -> if (ctx.development) CSPValue.UnsafeEval else null },
        CSPEntry { ctx -> if (ctx.development) CSPValue.Blob else null },
        CSPEntry(CSPValue.ReportSample),
    )

    private val styleSrc = listOf(
        CSPEntry(CSPValue.Self),
        // Due to using inline css with background-image url()
        CSPEntry(CSPValue.UnsafeInline),
        CSPEntry("maxcdn.bootstrapcdn.com"),
        CSPEntry("fonts.googleapis.com"),
        CSPEntry { ctx -> if (ctx.development) CSPValue.Blob else null },
        CSPEntry(CSPValue.ReportSample),
        CSPEntry("https://cdn.jsdelivr.net/npm/monaco-editor@0.25.2/"),
    )

    private val workerSrc = listOf(
        CSPEntry(CSPValue.Self),
        CSPEntry(CSPValue.Blob),
        CSPEntry(CSPValue.ReportSample),
    )

    private val mediaSrc = listOf(
        CSPEntry(CSPValue.Self),
        CSPEntry("https://dptr8y9slmfgv.cloudfront.net"),
        CSPEntry(CSPValue.ReportSample),
    )

    fun CSPContext.toCSPHeader(): String = buildString {
        fun appendList(name: String, entries: List<CSPEntry>, last: Boolean = false) {
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
            if (!last) {
                append(";")
            }
        }

        oneOffs
            .joinToString(" ")
            .ifBlank { null }
            ?.let { append("$it;") }

        appendList(CSPDirectives.DefaultSrc, defaultSrc)
        appendList(CSPDirectives.BaseUri, baseUri)
        appendList(CSPDirectives.FormAction, formAction)
        appendList(CSPDirectives.ManifestSrc, manifestSrc)
        appendList(CSPDirectives.ChildSrc, childSrc)
        appendList(CSPDirectives.ConnectSrc, connectSrc)
        appendList(CSPDirectives.FontSrc, fontSrc)
        appendList(CSPDirectives.FrameSrc, frameSrc)
        appendList(CSPDirectives.FrameAncestors, frameAncestors)
        appendList(CSPDirectives.ImgSrc, imgSrc)
        appendList(CSPDirectives.ObjectSrc, objectSrc)
        appendList(CSPDirectives.Sandbox, sandbox)
        appendList(CSPDirectives.ScriptSrc, scriptSrc)
        appendList(CSPDirectives.StyleSrc, styleSrc)
        appendList(CSPDirectives.WorkerSrc, workerSrc)
        appendList(CSPDirectives.MediaSrc, mediaSrc)
        appendList(CSPDirectives.ReportUri, reportUri, last = true)
    }
}
