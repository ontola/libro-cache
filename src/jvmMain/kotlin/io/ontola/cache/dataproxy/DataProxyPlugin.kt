package io.ontola.cache.dataproxy

import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.uri
import io.ontola.cache.plugins.logger

val DataProxyPlugin = createApplicationPlugin(name = "DataProxy", ::Configuration) {
    onCall { call ->
        val proxy = DataProxy(pluginConfig, call)
        call.attributes.put(DataProxyKey, proxy)
        val shouldProxyHttp = pluginConfig.shouldProxy(call.request)

        call.logger.debug {
            val uri = call.request.uri
            val rule = pluginConfig.matchOrDefault(uri)

            if (shouldProxyHttp)
                "Proxying request to backend: $uri, rule: $rule"
            else
                "Processing request: $uri, rule: $rule"
        }

        if (shouldProxyHttp) {
            proxy.interceptRequest(call)
        }
    }
}
