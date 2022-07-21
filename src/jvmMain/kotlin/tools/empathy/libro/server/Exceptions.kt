package tools.empathy.libro.server

class AuthenticationException : RuntimeException()

class AuthorizationException : RuntimeException()

class BadGatewayException : RuntimeException()

class TenantNotFoundException : RuntimeException() {
    override val message: String?
        get() = "Website not found"
}
