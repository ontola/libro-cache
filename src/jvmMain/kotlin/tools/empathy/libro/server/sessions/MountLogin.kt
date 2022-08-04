package tools.empathy.libro.server.sessions

import io.ktor.http.HttpHeaders
import io.ktor.server.application.call
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.header
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import tools.empathy.libro.server.tenantization.tenantOrNull
import java.lang.RuntimeException

fun Routing.mountLogin() {
    authenticate("libro-oidc") {
        get("/libro/login") {
            val postAuthRedirectLocation = call.tenantOrNull?.let { tenant ->
                call.request.header(HttpHeaders.Referrer)?.let { referer ->
                    if (referer.startsWith(tenant.websiteIRI.toString())) {
                        referer
                    } else {
                        null
                    }
                }
            }
            call.sessions.set(PreAuthSession(postAuthRedirectLocation))
            // Redirects to 'authorizeUrl' automatically
        }

        get("/libro/callback") {
            val principal: OAuthAccessTokenResponse.OAuth2? = call.principal()
            val session = if (principal != null) {
                println("Parameters: ${principal.extraParameters}")
                Session(
                    principal.accessToken,
                    principal.tokenType,
                    principal.refreshToken,
                )
            } else {
                EmptySession
            }
            val redirectLocation = try {
                call.sessions.get<PreAuthSession>()?.redirect ?: "/"
            } catch (e: RuntimeException) {
                "/"
            }
            call.sessions.set(session)

            call.respondRedirect(redirectLocation)
        }
    }
}
