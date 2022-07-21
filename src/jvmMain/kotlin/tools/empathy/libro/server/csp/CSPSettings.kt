package tools.empathy.libro.server.csp

import tools.empathy.libro.webmanifest.TrackerType

private fun TrackerType.canCallUA(): Boolean = this == TrackerType.GUA || this == TrackerType.GTM

object CSPSettings {
    private val self = arrayOf(
        CSPEntry(CSPValue.Self),
        CSPEntry { ctx -> if (ctx.development) "https://localhost" else null },
        CSPEntry { ctx -> if (ctx.development) "http://localhost:3001" else null },
    )

    private val oneOffs = listOf(
        CSPDirectives.UpgradeInsecureRequests,
    )

    private val defaultSrc = listOf(
        *self,
        CSPEntry { ctx -> ctx.manifest?.ontola?.csp?.defaultSrc?.joinToString(" ") },
        CSPEntry { ctx -> if (ctx.development) "https://localhost" else null },
        CSPEntry { ctx -> if (ctx.development) "http://localhost:3001" else null },
    )

    private val baseUri = listOf(
        *self,
        CSPEntry { ctx -> ctx.manifest?.ontola?.csp?.baseUri?.joinToString(" ") },
    )

    private val reportUri = listOf(
        CSPEntry(cspReportEndpointPath),
        CSPEntry { ctx -> ctx.manifest?.ontola?.csp?.reportUri?.joinToString(" ") },
    )

    private val formAction = listOf(
        *self,
        CSPEntry { ctx -> ctx.manifest?.ontola?.csp?.formAction?.joinToString(" ") },
    )

    private val manifestSrc = listOf(
        *self,
        CSPEntry(CSPValue.ReportSample),
        CSPEntry { ctx -> ctx.manifest?.ontola?.csp?.manifestSrc?.joinToString(" ") },
    )

    private val childSrc = listOf(
        CSPEntry("https://youtube.com"),
        CSPEntry("https://www.youtube.com"),
        CSPEntry(CSPValue.ReportSample),
        CSPEntry { ctx -> ctx.manifest?.ontola?.csp?.childSrc?.joinToString(" ") },
    )

    private val connectSrc = listOf(
        *self,
        CSPEntry { ctx -> ctx.manifest?.ontola?.csp?.connectSrc?.joinToString(" ") },
        CSPEntry("https://api.notubiz.nl"),
        CSPEntry("https://api.openraadsinformatie.nl"),
        CSPEntry("https://www.facebook.com"),
        CSPEntry("https://analytics.argu.co"),
        CSPEntry("https://dptr8y9slmfgv.cloudfront.net"),
        CSPEntry("https://maxcdn.bootstrapcdn.com"),
        CSPEntry("https://fonts.gstatic.com"),
        CSPEntry { ctx -> "ws://${ctx.host}" },
        CSPEntry { ctx -> "wss://${ctx.host}" },
        CSPEntry { ctx -> if (!ctx.development) "https://notify.bugsnag.com" else null },
        CSPEntry { ctx -> if (!ctx.development) "https://sessions.bugsnag.com" else null },
        CSPEntry { ctx -> if (ctx.manifest?.ontola?.tracking?.any { it.type.canCallUA() } == true) "https://www.google-analytics.com" else null },
    )

    private val fontSrc = listOf(
        *self,
        CSPEntry { ctx -> ctx.manifest?.ontola?.csp?.fontSrc?.joinToString(" ") },
        CSPEntry("https://maxcdn.bootstrapcdn.com"),
        CSPEntry("https://fonts.gstatic.com"),
        CSPEntry("https://cdn.jsdelivr.net/npm/monaco-editor@0.33.0/"),
    )

    private val frameSrc = listOf(
        CSPEntry { ctx -> ctx.manifest?.ontola?.csp?.frameSrc?.joinToString(" ") },
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
        *self,
        CSPEntry(CSPValue.Blob),
        CSPEntry(CSPValue.Data),
        CSPEntry("*"),
        CSPEntry { ctx -> ctx.manifest?.ontola?.csp?.imgSrc?.joinToString(" ") },
        CSPEntry("https://dptr8y9slmfgv.cloudfront.net"),
        CSPEntry { ctx -> if (ctx.manifest?.ontola?.tracking?.any { it.type.canCallUA() } == true) "https://www.google-analytics.com" else null },
        CSPEntry(CSPValue.ReportSample),
    )

    private val objectSrc = listOf(
        CSPEntry(CSPValue.None),
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
        CSPEntry { ctx -> ctx.manifest?.ontola?.csp?.sandbox?.joinToString(" ") },
    )

    private val scriptSrc = listOf(
        *self,
        CSPEntry(CSPValue.UnsafeEval),
        CSPEntry { ctx -> CSPValue.nonce(ctx.nonce) },
        CSPEntry { ctx -> ctx.manifest?.ontola?.csp?.scriptSrc?.joinToString(" ") },
        CSPEntry("https://cdn.polyfill.io"),
        // Bugsnag CDN
        CSPEntry("https://d2wy8f7a9ursnm.cloudfront.net"),
        CSPEntry("https://storage.googleapis.com"),
        CSPEntry("https://www.googletagmanager.com"),
        CSPEntry("https://webforms.pipedrive.com/f/loader"),
        CSPEntry("https://cdn.eu-central-1.pipedriveassets.com/leadbooster-chat/assets/web-forms/loader.min.js"),
        CSPEntry("https://browser-update.org"),
        CSPEntry("https://cdnjs.cloudflare.com"),
        CSPEntry("https://cdn.jsdelivr.net/npm/monaco-editor@0.33.0/"),
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
        *self,
        // Due to using inline css with background-image url()
        CSPEntry(CSPValue.UnsafeInline),
        CSPEntry { ctx -> ctx.manifest?.ontola?.csp?.styleSrc?.joinToString(" ") },
        CSPEntry("maxcdn.bootstrapcdn.com"),
        CSPEntry("fonts.googleapis.com"),
        CSPEntry { ctx -> if (ctx.development) CSPValue.Blob else null },
        CSPEntry(CSPValue.ReportSample),
        CSPEntry("https://cdn.jsdelivr.net/npm/monaco-editor@0.33.0/"),
    )

    private val workerSrc = listOf(
        *self,
        CSPEntry(CSPValue.Blob),
        CSPEntry(CSPValue.ReportSample),
        CSPEntry { ctx -> ctx.manifest?.ontola?.csp?.workerSrc?.joinToString(" ") },
    )

    private val mediaSrc = listOf(
        *self,
        CSPEntry("https://dptr8y9slmfgv.cloudfront.net"),
        CSPEntry(CSPValue.ReportSample),
        CSPEntry { ctx -> ctx.manifest?.ontola?.csp?.mediaSrc?.joinToString(" ") },
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
