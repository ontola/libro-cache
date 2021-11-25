package io.ontola.cache.plugins

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import java.util.UUID

object CSPSettings {
    val defaultSrc = listOf("'self'")

    val childSrc = listOf(
        "https://youtube.com",
        "https://www.youtube.com",
    )
    val connectSrc = listOf(
        "'self'",
        "https://api.notubiz.nl",
        "https://api.openraadsinformatie.nl",
        "https://www.facebook.com",
        "https://analytics.argu.co",
        "https://argu-logos.s3.eu-central-1.amazonaws.com",
//        (req) => `ws://${req.hostname}`,
    )

    val fontSrc = listOf(
        "'self'",
        "https://maxcdn.bootstrapcdn.com",
        "https://fonts.gstatic.com",
    )
    val frameSrc = listOf(
        "https://youtube.com",
        "https://www.youtube.com",
        "https://*.typeform.com/",
        "https://webforms.pipedrive.com",
    )
    val imgSrc = listOf(
        "'self'",
        "blob:",
        "data:",
        "*",
    )
    val objectSrc = listOf("'none'")
    val sandbox = listOf(
        "allow-downloads",
        "allow-forms",
        "allow-modals",
        "allow-popups",
        "allow-popups-to-escape-sandbox",
        "allow-presentation",
        "allow-same-origin",
        "allow-scripts",
    )
    val scriptSrc = listOf(
        "'self'",
        "'unsafe-eval'",
//        (req, res) => `'nonce-${res.locals.nonce}'`,
        "https://cdn.polyfill.io",
        // Bugsnag CDN
        "https://d2wy8f7a9ursnm.cloudfront.net",
        "https://storage.googleapis.com",
        "https://www.googletagmanager.com",
        "https://webforms.pipedrive.com/f/loader",
        "https://cdn.eu-central-1.pipedriveassets.com/leadbooster-chat/assets/web-forms/loader.min.js",
        "https://browser-update.org",
        "https://argu-logos.s3.eu-central-1.amazonaws.com",
        "https://cdnjs.cloudflare.com",
//        (req) => {
//            val { manifest } = req.getCtx()
//
//            if (!Array.isArray(manifest?.ontola?.tracking)) {
//                return undefined
//            }
//
//            return manifest
//                .ontola
//                .tracking
//                .filter(({ type }) => type === 'Matomo' || type === 'PiwikPro')
//            .map(({ host }) => `https://${host}`)
//            .join(' ')
//        },
    )

    val styleSrc = listOf(
        "'self'",
        // Due to using inline css with background-image url()
        "'unsafe-inline'",
        "maxcdn.bootstrapcdn.com",
        "fonts.googleapis.com",
    )
    val workerSrc = listOf(
        "'self'",
    )
    val mediaSrc = listOf(
        "'self'",
    )

//    init {
//        if (assetsHost) {
//            workerSrc.push(assetsHost);
//        }
//
//        if (false) { // __DEVELOPMENT__
//            scriptSrc.push("'unsafe-inline'", "'unsafe-eval'", 'blob:')
//            styleSrc.push("blob:")
//            workerSrc.push("blob:")
//            mediaSrc.push("https://argu.localdev")
//            mediaSrc.push("http://localhost:3001")
//        }
//
//        if (['production', 'staging', 'test'].includes(process.env.NODE_ENV)) {
//            connectSrc.push("https://notify.bugsnag.com")
//            connectSrc.push("https://sessions.bugsnag.com")
//        }
//    }
}

class CSP(private val configuration: Configuration) {
    class Configuration

    private fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        val nonce = UUID.randomUUID().toString()
        context.call.attributes.put(CSPKey, nonce)

        // TODO: port headers
        // context.call.response.header("Content-Security-Policy", buildCSPHeader(nonce))
    }

    fun buildCSPHeader(nonce: String) {

    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, CSP> {
        override val key = AttributeKey<CSP>("CSP")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): CSP {
            val configuration = Configuration().apply(configure)
            val feature = io.ontola.cache.plugins.CSP(configuration)

            pipeline.intercept(ApplicationCallPipeline.Features) {
                feature.intercept(this)
            }

            return feature
        }
    }
}

private val CSPKey = AttributeKey<String>("CSPKey")

internal val ApplicationCall.nonce: String
    get() = attributes.getOrNull(CSPKey) ?: reportMissingNonce()

private fun ApplicationCall.reportMissingNonce(): Nothing {
    application.feature(CacheSession) // ensure the feature is installed
    throw NonceNotYetConfiguredException()
}

class NonceNotYetConfiguredException :
    IllegalStateException("Nonce is not yet ready: you are asking it to early before the CSP feature.")
