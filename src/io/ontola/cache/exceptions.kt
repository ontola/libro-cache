package io.ontola.cache

//class AuthenticationException(msg: String? = null) : RuntimeException(msg)
class TenantNotFoundException : RuntimeException() {
    override val message: String?
        get() = "Website not found"
}
class BadGatewayException : RuntimeException()
//class AuthorizationException : RuntimeException()
