package io.ontola.cache.csp

class CSPEntry(val constant: String? = null, val dynamic: ((ctx: CSPContext) -> String?)? = null)
