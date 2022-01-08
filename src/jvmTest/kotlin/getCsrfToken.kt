
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest

fun TestApplicationEngine.getCsrfToken(): String {
    val call = handleRequest(HttpMethod.Get, "/_testing/csrfToken") {
        addHeader(HttpHeaders.XForwardedProto, "https")
    }

    return call.response.content!!
}
