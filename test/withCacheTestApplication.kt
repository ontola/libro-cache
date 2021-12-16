
import io.ktor.config.MapApplicationConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.cookiesSession
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withApplication
import io.ontola.cache.module
import io.ontola.cache.plugins.CacheConfig
import io.ontola.cache.plugins.StorageAdapter
import io.ontola.cache.sessions.SessionData
import io.ontola.cache.tenantization.Manifest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals

data class TestContext(
    val adapter: StorageAdapter<String, String>,
    val config: CacheConfig,
)

data class MockConfiguration(
    val clientBuilder: TestClientBuilder,
    val storage: TestStorageAdapterBuilder,
    var initialAccessTokens: Pair<String, String>? = null,
) {
    fun addManifest(website: Url, manifest: Manifest) {
        storage.addManifest(website, manifest)
        clientBuilder.addManifest(website, manifest)
    }
}

fun <R> withCacheTestApplication(
    configure: MockConfiguration.() -> Unit = {},
    test: TestApplicationEngine.(context: TestContext) -> R,
) {
    val env = createTestEnvironment {
        (this.config as MapApplicationConfig).apply {
            put("ktor.deployment.port", "3080")
        }
    }

    val clientBuilder = TestClientBuilder {
        refreshTokenSuccess
    }
    val client = clientBuilder.build()
    val config = CacheConfig.fromEnvironment(env.config, true, client)
    val adapterBuilder = TestStorageAdapterBuilder(config)

    val mockConfig = MockConfiguration(clientBuilder, adapterBuilder)
        .apply {
            clientBuilder.config.initialKeys = initialAccessTokens
        }
        .apply { configure() }
    val adapter = adapterBuilder.build()
    val context = TestContext(adapter, config)

    withApplication(
        env,
        test = {
            application.module(
                testing = true,
                storage = adapter,
                persistentStorage = adapter,
                client = client,
            )

            cookiesSession {

                mockConfig.initialAccessTokens?.let {
                    handleRequest(HttpMethod.Post, "/_testing/setSession") {
                        addHeader("Accept", "application/json")
                        addHeader("Content-Type", "application/json")
                        addHeader(HttpHeaders.XForwardedProto, "https")

                        setBody(Json.encodeToString(SessionData(it.first, it.second)))
                    }.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                    }
                }

                test(context)
            }
        },
    )
}
