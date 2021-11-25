package io.ontola.cache.plugins

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.application.call
import io.ktor.application.feature
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import io.ktor.sessions.set
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import java.util.UUID

//const oneYearInMiliSec = 31536000000;
//
//const deviceIdFromCookie = (ctx) => ctx.cookies.get('deviceId');
//
//const generateDeviceId = () => uuidv4();
//
//const deviceIdMiddleware = async (ctx, next) => {
//    if (!ctx.secure) {
//        return next();
//    }
//
//    let deviceId = deviceIdFromCookie(ctx);
//
//    if (!deviceId) {
//        deviceId = generateDeviceId();
//        ctx.cookies.set('deviceId', deviceId, {
//                httpOnly: true,
//                maxAge: oneYearInMiliSec,
//                secure: true,
//        });
//    }
//
//    ctx.deviceId = deviceId;
//
//    return next();
//};

class DeviceId(private val configuration: Configuration) {
    class Configuration

    private fun intercept(context: PipelineContext<Unit, ApplicationCall>) {
        val deviceId = context.call.sessions.get<String>() ?: generateSessionId(context)
        context.call.attributes.put(DeviceIdKey, deviceId)
    }

    private fun generateSessionId(context: PipelineContext<Unit, ApplicationCall>): String {
        val id = UUID.randomUUID().toString()
        context.call.sessions.set(id)

        return id
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, DeviceId> {
        override val key = AttributeKey<DeviceId>("DeviceId")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): DeviceId {
            val configuration = Configuration().apply(configure)
            val feature = DeviceId(configuration)

            pipeline.intercept(ApplicationCallPipeline.Features) {
                feature.intercept(this)
            }

            return feature
        }
    }
}

private val DeviceIdKey = AttributeKey<String>("DeviceIdKey")

internal val ApplicationCall.deviceId: String
    get() = attributes.getOrNull(DeviceIdKey) ?: reportMissingDeviceId()

private fun ApplicationCall.reportMissingDeviceId(): Nothing {
    application.feature(CacheSession) // ensure the feature is installed
    throw DeviceIdNotYetConfiguredException()
}

class DeviceIdNotYetConfiguredException :
    IllegalStateException("DeviceId is not yet ready: you are asking it to early before the DeviceId feature.")
