package tools.empathy.libro.server.csp

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.application
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import tools.empathy.libro.server.plugins.cacheConfig

class CSPReportException(
    val report: CSPReport,
    override val message: String?,
) : Exception(message)

@Serializable
data class CSPReportEnvelope(
    @SerialName("csp-report")
    val cspReport: CSPReport
)

@Serializable
data class CSPReport(
    @SerialName("blocked-uri")
    val blockedUri: String? = null,
    @SerialName("column-number")
    val columnNumber: Int? = null,
    @SerialName("document-uri")
    val documentUri: String? = null,
    @SerialName("line-number")
    val lineNumber: Int? = null,
    @SerialName("original-policy")
    val originalPolicy: String? = null,
    @SerialName("referrer")
    val referrer: String? = null,
    @SerialName("script-sample")
    val scriptSample: String? = null,
    @SerialName("source-file")
    val sourceFile: String? = null,
    @SerialName("violated-directive")
    val violatedDirective: String? = null,
)

const val cspReportEndpointPath = "/csp-reports"

@OptIn(ExperimentalSerializationApi::class)
private val lenientSerializer = Json {
    isLenient = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

fun Routing.mountCSP() {
    post(cspReportEndpointPath) {
        val raw = call.receiveText()
        val body = lenientSerializer.decodeFromString<CSPReportEnvelope>(raw)

        application.cacheConfig.notify(CSPReportException(body.cspReport, raw))

        call.respond(HttpStatusCode.OK)
    }
}
