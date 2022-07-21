package tools.empathy.libro.server

import io.micrometer.core.instrument.Counter
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

object Metrics {
    private const val prefix = "libro-server"
    val metricsRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val messages: Counter = metricsRegistry.counter("${prefix}_invalidator_messages")
    val invalidations: Counter = metricsRegistry.counter("${prefix}_invalidations_total")
}
