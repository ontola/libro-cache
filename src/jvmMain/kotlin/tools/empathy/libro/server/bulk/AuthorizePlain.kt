package tools.empathy.libro.server.bulk

import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.http.Url
import io.ktor.http.fullPath
import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import tools.empathy.libro.server.configuration.libroConfig
import tools.empathy.libro.server.plugins.language
import tools.empathy.libro.server.plugins.services
import tools.empathy.libro.server.util.measured

internal suspend fun ApplicationCall.authorizePlain(
    resources: List<String>,
): Flow<SPIResourceResponseItem> = measured("authorizePlain;i=${resources.size}") {
    val lang = language

    resources
        .asFlow()
        .map {
            it to measured("authorizePlain - $it") {
                application.libroConfig.client.get {
                    url(services.route(Url(it).fullPath))
                    initHeaders(this@authorizePlain, lang)
                    headers {
                        header("Accept", "application/empathy+json")
                    }
                    expectSuccess = false
                }
            }
        }
        .map { (iri, response) ->
            SPIResourceResponseItem(
                iri = iri,
                status = response.status.value,
                cache = CacheControl.Private,
                language = lang,
                body = response.body()
            )
        }
}
