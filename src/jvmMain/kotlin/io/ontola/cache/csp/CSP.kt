package io.ontola.cache.csp

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.plugin
import io.ktor.server.request.host
import io.ktor.server.response.header
import io.ktor.util.AttributeKey
import io.ontola.cache.csp.CSPSettings.toCSPHeader
import io.ontola.cache.plugins.CacheSession
import io.ontola.cache.plugins.blacklisted
import io.ontola.cache.tenantization.tenant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
private data class ReportEndpoint(val url: String)

@Serializable
private data class ReportToEntry(
    val group: String,
    @SerialName("max-age")
    val maxAge: Long,
    val endpoints: List<ReportEndpoint>,
)

val CSP = createApplicationPlugin("CSP") {
    val isDevelopment = application.developmentMode

    val cspReportEntry = ReportToEntry(
        "csp-endpoint",
        10886400,
        listOf(
            ReportEndpoint("/csp-reports"),
        ),
    )
    val cspReportToHeader = Json.encodeToString(cspReportEntry)

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
        call.response.header("Report-To", cspReportToHeader)
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
