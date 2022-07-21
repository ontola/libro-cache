package tools.empathy.libro.server.csp

import tools.empathy.libro.webmanifest.Manifest

class CSPContext(
    val development: Boolean,
    val nonce: String,
    val host: String,
    val manifest: Manifest?,
)
