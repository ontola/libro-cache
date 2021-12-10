
import io.ktor.config.MapApplicationConfig
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.withApplication
import io.ontola.cache.module
import io.ontola.cache.plugins.CacheConfig
import io.ontola.cache.plugins.StorageAdapter

data class TestContext(
    val adapter: StorageAdapter<String, String>,
    val config: CacheConfig,
)

data class MockObjects(
    val clientBuilder: TestClientBuilder,
    val storage: TestStorageAdapterBuilder,
)

fun <R> withCacheTestApplication(
    configure: MockObjects.() -> Unit = {},
    test: TestApplicationEngine.(context: TestContext) -> R,
) {
    val env = createTestEnvironment {
        (this.config as MapApplicationConfig).apply {
            put("ktor.deployment.port", "3080")
        }
    }

    val clientBuilder = TestClientBuilder()
    val client = clientBuilder.build()
    val config = CacheConfig.fromEnvironment(env.config, true, client)
    val adapterBuilder = TestStorageAdapterBuilder(config)

    MockObjects(clientBuilder, adapterBuilder).configure()
    val adapter = adapterBuilder.build()
    val context = TestContext(adapter, config)

    withApplication(
        env,
        test = {
            application.module(testing = true, storage = adapter, client = client)
            test(context)
        },
    )
}
