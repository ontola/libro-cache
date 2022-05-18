package io.ontola.cache

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry

fun initializeOpenTelemetry(): OpenTelemetry {
    return GlobalOpenTelemetry.get()
}
