package io.ontola.util

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIOEngineConfig
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

fun HttpClientConfig<CIOEngineConfig>.disableCertValidation() {
    engine {
        https {
            trustManager = object : X509TrustManager {
                override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) { }

                override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) { }

                override fun getAcceptedIssuers(): Array<X509Certificate>? = null
            }
        }
    }
}
