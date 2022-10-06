package tools.empathy.libro.server.csp

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.plugin
import io.ktor.server.request.host
import io.ktor.server.response.header
import io.ktor.util.AttributeKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tools.empathy.libro.server.csp.CSPSettings.toCSPHeader
import tools.empathy.libro.server.plugins.CacheSession
import tools.empathy.libro.server.plugins.blacklisted
import tools.empathy.libro.server.tenantization.tenantOrNull
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
        if (call.blacklisted)
            return@onCall

        val nonce = UUID.randomUUID().toString()
        call.attributes.put(CSPKey, nonce)

        val ctx = CSPContext(
            isDevelopment,
            nonce,
            call.request.host(),
            if (call.blacklisted) null else call.tenantOrNull?.manifest,
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
