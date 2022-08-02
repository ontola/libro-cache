@file:UseSerializers(UrlSerializer::class)

package tools.empathy.libro.server.sessions.oidc

import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import tools.empathy.libro.server.util.UrlSerializer

@Serializable
enum class OIDCResponseType {
    @SerialName("none")
    None,

    @SerialName("code")
    Code,

    @SerialName("token")
    Token,

    @SerialName("id_token")
    IdToken,

    @SerialName("id_token token")
    IdTokenToken,
}

@Serializable
enum class OIDCResponseMode {
    @SerialName("query")
    Query,

    @SerialName("fragment")
    Fragment,

    @SerialName("form_post")
    FormPost,
}

@Serializable
enum class OIDCGrantType {
    @SerialName("authorization_code")
    AuthorizationCode,

    @SerialName("password")
    Password,

    @SerialName("client_credentials")
    ClientCredentials,

    @SerialName("implicit_oidc")
    ImplicitOidc,

    @SerialName("refresh_token")
    RefreshToken,
}

@Serializable
data class OpenIdConfiguration(
    val issuer: Url,
    @SerialName("authorization_endpoint")
    val authorizationEndpoint: Url,
    @SerialName("token_endpoint")
    val tokenEndpoint: Url,
    @SerialName("registration_endpoint")
    val registrationEndpoint: Url,
    @SerialName("revocation_endpoint")
    val revocationEndpoint: Url,
    @SerialName("introspection_endpoint")
    val introspectionEndpoint: Url,
    @SerialName("userinfo_endpoint")
    val userinfoEndpoint: Url,
    @SerialName("jwks_uri")
    val jwksUri: Url,

    @SerialName("scopes_supported")
    val scopesSupported: List<String>,
    @SerialName("response_types_supported")
    val responseTypesSupported: List<OIDCResponseType>,
    @SerialName("response_modes_supported")
    val responseModesSupported: List<OIDCResponseMode>,
    @SerialName("grant_types_supported")
    val grantTypesSupported: List<OIDCGrantType>,
    @SerialName("token_endpoint_auth_methods_supported")
    val tokenEndpointAuthMethodsSupported: List<String>,
    @SerialName("subject_types_supported")
    val subjectTypesSupported: List<String>,
    @SerialName("id_token_signing_alg_values_supported")
    val idTokenSigningAlgValuesSupported: List<String>,
    @SerialName("claim_types_supported")
    val claimTypesSupported: List<String>,
    @SerialName("claims_supported")
    val claimsSupported: List<String>,
)
