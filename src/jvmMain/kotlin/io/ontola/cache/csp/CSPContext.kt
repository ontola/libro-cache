package io.ontola.cache.csp

import io.ontola.apex.webmanifest.Manifest

class CSPContext(
    val development: Boolean,
    val nonce: String,
    val host: String,
    val manifest: Manifest?,
)
