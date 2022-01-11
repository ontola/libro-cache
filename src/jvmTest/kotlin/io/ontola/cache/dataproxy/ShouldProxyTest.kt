package io.ontola.cache.dataproxy

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.withApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ShouldProxyTest {
    private val emptyConfig = Configuration()

    @Test
    fun shouldProxyIncludedDataExtension() {
        val config = Configuration().apply {
            extensions = listOf("ext")
        }
        val request = createRequest(uri = "/tenant/forum.ext")
        val doubleExtensionRequest = createRequest(uri = "/tenant/forum.2022.ext")

        assertEquals(true, config.shouldProxy(request))
        assertEquals(true, config.shouldProxy(doubleExtensionRequest))
        assertEquals(false, emptyConfig.shouldProxy(request))
    }

    @Test
    fun shouldNotProxyManifestRequests() {
        Configuration().apply {
            methods = listOf(HttpMethod.Get)
        }
        val rootRequest = createRequest(uri = "/manifest.json")
        val nestedRequest = createRequest(uri = "/tenant/manifest.json")

        assertEquals(false, emptyConfig.shouldProxy(rootRequest))
        assertEquals(false, emptyConfig.shouldProxy(nestedRequest))
    }

    @Test
    fun shouldProxyExcludedPaths() {
        val config = Configuration().apply {
            methods = listOf(HttpMethod.Get)
            rules = listOf(
                ProxyRule(Regex("/tenant/excluded$"), exclude = true),
                ProxyRule(Regex("/excludedSegment/"), exclude = true),
            )
        }
        val excludedRequest = createRequest(uri = "/tenant/excluded")
        val includedSubresourceRequest = createRequest(uri = "/tenant/excluded/resource")

        val segmentRequest = createRequest(uri = "/excludedSegment/")
        val segmentSubresourceRequest = createRequest(uri = "/excludedSegment/resource")

        assertEquals(false, config.shouldProxy(excludedRequest))
        assertEquals(true, config.shouldProxy(includedSubresourceRequest))
        assertEquals(false, config.shouldProxy(segmentRequest))
        assertEquals(false, config.shouldProxy(segmentSubresourceRequest))
    }

    @Test
    fun shouldProxyDataAccept() {
        val config = Configuration().apply {
            contentTypes = listOf(
                ContentType.parse("application/ld+json"),
            )
        }
        val jsonLdRequest = createRequest(uri = "/forum") {
            addHeader(HttpHeaders.Accept, "application/ld+json")
        }

        assertEquals(false, emptyConfig.shouldProxy(jsonLdRequest))
        assertEquals(true, config.shouldProxy(jsonLdRequest))
    }

    @Test
    fun shouldProxySelectedMethods() {
        val config = Configuration().apply {
            methods = listOf(HttpMethod.Put)
        }
        val getRequest = createRequest(httpMethod = HttpMethod.Get)
        val putRequest = createRequest(httpMethod = HttpMethod.Put)

        assertEquals(false, config.shouldProxy(getRequest))
        assertEquals(true, config.shouldProxy(putRequest))
        assertEquals(false, emptyConfig.shouldProxy(getRequest))
        assertEquals(false, emptyConfig.shouldProxy(putRequest))
    }

    @Test
    fun shouldProxyDownloadRequests() {
        val regularRequest = createRequest()
        val downloadRequest = createRequest(uri = "/?download=true")

        assertEquals(false, emptyConfig.shouldProxy(regularRequest))
        assertEquals(true, emptyConfig.shouldProxy(downloadRequest))
    }
}

private fun createRequest(
    httpMethod: HttpMethod = HttpMethod.Get,
    uri: String = "/",
    setup: TestApplicationRequest.() -> Unit = {},
): ApplicationRequest {
    val env = applicationEngineEnvironment { }

    return withApplication(environment = env, test = {
        createCall(
            readResponse = true,
            setup = {
                this.method = httpMethod
                this.uri = uri
                setup()
            }
        ).request
    })
}
