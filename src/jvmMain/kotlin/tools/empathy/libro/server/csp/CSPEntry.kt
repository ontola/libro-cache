package tools.empathy.libro.server.csp

class CSPEntry(val constant: String? = null, val dynamic: ((ctx: CSPContext) -> String?)? = null)
