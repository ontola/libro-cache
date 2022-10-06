/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*
* Modified to support runtime cookie path
*/

package io.ktor.server.dynamicCookie

import io.ktor.server.sessions.CookieConfiguration
import io.ktor.server.sessions.SessionProvider
import io.ktor.server.sessions.SessionSerializer
import io.ktor.server.sessions.SessionStorage
import io.ktor.server.sessions.SessionTrackerById
import io.ktor.server.sessions.SessionTransportTransformer
import io.ktor.server.sessions.SessionsConfig
import io.ktor.server.sessions.defaultSessionSerializer
import io.ktor.server.sessions.generateSessionId
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

@PublishedApi
internal fun <S : Any> SessionsConfig.dynamicCookie(
    name: String,
    builder: CookieIdSessionBuilder<S>,
    sessionType: KClass<S>,
    storage: SessionStorage
) {
    val transport = SessionTransportCookie(name, builder.cookie, builder.transformers)
    val tracker = SessionTrackerById(sessionType, builder.serializer, storage, builder.sessionIdProvider)
    val provider = SessionProvider(name, sessionType, transport, tracker)
    register(provider)
}

public inline fun <reified S : Any> SessionsConfig.dynamicCookie(
    name: String,
    storage: SessionStorage,
    block: CookieIdSessionBuilder<S>.() -> Unit
) {
    val sessionType = S::class

    val builder = CookieIdSessionBuilder(sessionType, typeOf<S>()).apply(block)
    dynamicCookie(name, builder, sessionType, storage)
}

public class CookieIdSessionBuilder<S : Any>
@PublishedApi
internal constructor(
    type: KClass<S>,
    typeInfo: KType
) : CookieSessionBuilder<S>(type, typeInfo) {

    @Deprecated("Use builder functions instead.", level = DeprecationLevel.ERROR)
    public constructor(type: KClass<S>) : this(type, type.starProjectedType)

    /**
     * Register session ID generation function
     */
    public fun identity(f: () -> String) {
        sessionIdProvider = f
    }

    /**
     * Current session ID provider function
     */
    public var sessionIdProvider: () -> String = { generateSessionId() }
        private set
}

public open class CookieSessionBuilder<S : Any>
@PublishedApi
internal constructor(
    public val type: KClass<S>,
    public val typeInfo: KType
) {
    @Deprecated("Use builder functions instead.", level = DeprecationLevel.ERROR)
    public constructor(type: KClass<S>) : this(type, type.starProjectedType)

    /**
     * Session instance serializer
     */
    public var serializer: SessionSerializer<S> = defaultSessionSerializer(typeInfo)

    private val _transformers = mutableListOf<SessionTransportTransformer>()

    /**
     * Registered session transformers
     */
    public val transformers: List<SessionTransportTransformer> get() = _transformers

    /**
     * Register a session [transformer]. Useful for encryption, signing and so on
     */
    public fun transform(transformer: SessionTransportTransformer) {
        _transformers.add(transformer)
    }

    /**
     * Cookie header configuration
     */
    public val cookie: CookieConfiguration = CookieConfiguration()
}
