package io.ontola.cache.invalidator

import io.ktor.application.*
import io.ktor.util.*
import io.lettuce.core.*
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.ontola.cache.features.CacheConfig
import io.ontola.cache.features.RedisConfig
import io.ontola.transactions.Created
import io.ontola.transactions.Deleted
import io.ontola.transactions.Updated
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import java.net.InetAddress
import kotlin.concurrent.thread

const val REDIS_INFO_NAME_INDEX = 1

@OptIn(ExperimentalLettuceCoroutinesApi::class)
suspend fun streamAndGroupExist(redisConn: RedisCoroutinesCommands<String, String>, config: RedisConfig): Boolean {
    if (redisConn.exists(config.invalidationChannel) == 0L) {
        return false
    }
    val groupInfo = redisConn.xinfoGroups(config.invalidationChannel) as List<List<Any>>

    return groupInfo.any { info -> info[REDIS_INFO_NAME_INDEX] == config.invalidationGroup }
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
suspend fun createGroupAndStream(redisConn: RedisCoroutinesCommands<String, String>, config: RedisConfig) {
    val enableStreamCreation = XGroupCreateArgs().apply {
        mkstream(true)
    }

    redisConn.xgroupCreate(
        XReadArgs.StreamOffset.latest(config.invalidationChannel),
        config.invalidationGroup,
        enableStreamCreation,
    )
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
suspend fun ensureConsumer(redisConn: RedisCoroutinesCommands<String, String>, config: RedisConfig, consumer: Consumer<String>) {
    val consumers = redisConn.xinfoConsumers(config.invalidationChannel, config.invalidationGroup) as List<List<Any>>
    if (consumers.none { info -> info[REDIS_INFO_NAME_INDEX] == consumer.name }) {
        redisConn.xgroupCreateconsumer(config.invalidationChannel, consumer).let {
            if (it === null || !it) {
                throw Exception("Could not create consumer")
            }
        }
    }
}

@OptIn(KtorExperimentalAPI::class, ExperimentalLettuceCoroutinesApi::class)
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    thread {
        println("Invalidator called")
        val config = CacheConfig.fromEnvironment(environment.config, testing)
        println("Config: $config")

        val redis = RedisClient.create(config.libroRedisURI)
        val redisConn = redis.connect().coroutines()

        val consumer = Consumer.from(config.redis.invalidationGroup, InetAddress.getLocalHost().hostName)
        val stream = XReadArgs.StreamOffset.lastConsumed(config.redis.invalidationChannel)

        runBlocking {
            if (!streamAndGroupExist(redisConn, config.redis)) {
               createGroupAndStream(redisConn, config.redis)
            }

            try {
                ensureConsumer(redisConn, config.redis, consumer)

                val args = XReadArgs().apply {
                    block(Long.MAX_VALUE)
                }
                while(true) {
                    redisConn
                        .xreadgroup(consumer, args, stream)
                        .collect { msg ->
                            println("msg: $msg")
                            val resource = msg.body["resource"] ?: throw SerializationException("Message missing key 'resource'")
                            val resourceType = msg.body["resourceType"] ?: throw SerializationException("Message missing key 'resourceType'")

                            when (msg.body["type"]) {
                                "io.ontola.transactions.Created" -> {
                                    Created(
                                        resource = resource,
                                        resourceType = resourceType,
                                    )
                                }
                                "io.ontola.transactions.Updated" -> {
                                    Updated(
                                        resource = resource,
                                        resourceType = resourceType,
                                    )
                                }
                                "io.ontola.transactions.Deleted" -> {
                                    Deleted(
                                        resource = resource,
                                        resourceType = resourceType,
                                    )
                                }
                            }
                        }
                }
            } finally {
                redisConn.xgroupDelconsumer(config.redis.invalidationChannel, consumer)
            }

            println("quit?? T.T")
        }
    }
}
