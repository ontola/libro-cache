package io.ontola.cache.csp

object CSPValue {
    const val Self = "'self'"
    const val Blob = "blob:"
    const val Data = "data:"
    const val None = "'none'"
    const val UnsafeEval = "'unsafe-eval'"
    const val UnsafeInline = "'unsafe-inline'"
    const val ReportSample = "'report-sample'"

    fun nonce(nonce: String) = "'nonce-$nonce'"
}
