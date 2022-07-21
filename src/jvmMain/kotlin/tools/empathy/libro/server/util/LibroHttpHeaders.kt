package tools.empathy.libro.server.util

const val VaryHeader = "Accept,Accept-Encoding,Authorization,Content-Type"

object LibroHttpHeaders {
    const val ExecAction = "Exec-Action"
    const val IncludeResources = "Include-Resources"
    const val NewAuthorization = "New-Authorization"
    const val NewRefreshToken = "New-Refresh-Token"
    const val WebsiteIri = "Website-IRI"
    const val XDeviceId = "X-Device-Id"
    const val XCsrfToken = "X-CSRF-Token"
    const val XAPIVersion = "X-API-Version"
    const val XClientVersion = "X-Client-Version"
    const val XServerVersion = "X-Server-Version"

    /**
     * Used in forms to refer to the resource which sprung the request.
     */
    const val RequestReferrer = "Request-Referrer"
}
