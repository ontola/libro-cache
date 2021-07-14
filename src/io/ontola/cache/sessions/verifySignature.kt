package io.ontola.cache.sessions

import io.ktor.util.hex
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils

fun verifySignature(
    cookieNameLegacy: String,
    sessionSecret: String,
    sessionId: String,
    signature: String?,
): Boolean {
    val checkInput = "$cookieNameLegacy=$sessionId".toByteArray()
    val commo = HmacUtils(HmacAlgorithms.HMAC_SHA_1, sessionSecret).hmacHex(checkInput)
    // https://github.com/tj/node-cookie-signature/blob/master/index.js#L23
    val final = Base64().encodeAsString(hex(commo))
        .replace('/', '_')
        .replace('+', '-')
        .replace("=", "")

    return signature == final
}
