package io.ontola.cache.util

import io.ktor.application.ApplicationCall
import io.ktor.http.Url
import io.ktor.request.uri
import io.ontola.cache.plugins.tenant

fun ApplicationCall.requestUri(): Url = tenant.websiteOrigin.copy(encodedPath = request.uri)
