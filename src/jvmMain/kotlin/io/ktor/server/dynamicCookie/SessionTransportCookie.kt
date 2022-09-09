/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.dynamicCookie

import io.ktor.http.Cookie
import io.ktor.server.application.ApplicationCall
import io.ktor.server.sessions.CookieConfiguration
import io.ktor.server.sessions.SessionTransport
import io.ktor.server.sessions.SessionTransportTransformer
import io.ktor.server.sessions.transformRead
import io.ktor.server.sessions.transformWrite
import io.ktor.util.AttributeKey
import io.ktor.util.date.GMTDate
import io.ktor.util.date.plus

@PublishedApi
internal val CookieOverridesKey = AttributeKey<CookieConfiguration>("CookieOverrides")

public fun ApplicationCall.overrideCookie(init: CookieConfiguration.() -> Unit) {
    attributes.put(CookieOverridesKey, CookieConfiguration().apply(init))
}

/**
 * SessionTransport that adds a Set-Cookie header and reads Cookie header
 * for the specified cookie [name], and a specific cookie [configuration] after
 * applying/un-applying the specified transforms defined by [transformers].
 *
 * @property name is a cookie name
 * @property configuration is a cookie configuration
 * @property transformers is a list of session transformers
 */
public class SessionTransportCookie(
    public val name: String,
    public val configuration: CookieConfiguration,
    public val transformers: List<SessionTransportTransformer>
) : SessionTransport {

    override fun receive(call: ApplicationCall): String? {
        val config = call.attributes.getOrNull(CookieOverridesKey) ?: configuration

        return transformers.transformRead(call.request.cookies[name, config.encoding])
    }

    override fun send(call: ApplicationCall, value: String) {
        val config = call.attributes.getOrNull(CookieOverridesKey) ?: configuration

        val now = GMTDate()
        val maxAge = config.maxAgeInSeconds
        val expires = when {
            maxAge == 0L -> null
            else -> now + maxAge * 1000L
        }

        val cookie = Cookie(
            name,
            transformers.transformWrite(value),
            config.encoding,
            maxAge.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            expires,
            config.domain,
            config.path,
            config.secure,
            config.httpOnly,
            config.extensions
        )

        call.response.cookies.append(cookie)
    }

    override fun clear(call: ApplicationCall) {
        val config = call.attributes.getOrNull(CookieOverridesKey) ?: configuration
        call.response.cookies.appendExpired(name, config.domain, config.path)
    }

    override fun toString(): String {
        return "SessionTransportCookie: $name"
    }
}
