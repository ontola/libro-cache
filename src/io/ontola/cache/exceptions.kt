package io.ontola.cache

class AuthenticationException : RuntimeException()

class AuthorizationException : RuntimeException()

class BadGatewayException : RuntimeException()

class TenantNotFoundException : RuntimeException() {
    override val message: String?
        get() = "Website not found"
}
