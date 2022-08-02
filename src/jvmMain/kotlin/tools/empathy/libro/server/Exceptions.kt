package tools.empathy.libro.server

class AuthenticationException : RuntimeException()

class AuthorizationException : RuntimeException()

class BadGatewayException : RuntimeException()

class TenantNotFoundException : RuntimeException() {
    override val message: String?
        get() = "Website not found"
}

class WrongWebsiteIRIException : RuntimeException() {
    override val message: String?
        get() = "Website-Iri does not correspond with authority headers"
}
