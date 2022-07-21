package io.ontola.cache.util

import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.encodedPath
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.uri
import io.ontola.cache.tenantization.tenant

fun ApplicationCall.requestUriFromTenant(): Url = URLBuilder(tenant.websiteOrigin)
    .apply { encodedPath = request.uri }
    .build()
