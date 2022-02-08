package io.ontola.cache.invalidator

import io.ktor.server.application.Application
import io.lettuce.core.Consumer
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.XGroupCreateArgs
import io.lettuce.core.XReadArgs
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.ontola.cache.Metrics
import io.ontola.cache.plugins.CacheConfig
import io.ontola.cache.plugins.RedisConfig
import io.ontola.cache.util.KeyManager
import io.ontola.transactions.Deleted
import io.ontola.transactions.Updated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import mu.KotlinLogging
import java.net.InetAddress
import kotlin.concurrent.thread

const val REDIS_INFO_NAME_INDEX = 1

val logger = KotlinLogging.logger {}

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
suspend fun ensureConsumer(redisConn: RedisCoroutinesCommands<String, String>, channel: String, consumer: Consumer<String>) {
    val consumers = redisConn.xinfoConsumers(channel, consumer.group) as List<List<Any>>
    if (consumers.none { info -> info[REDIS_INFO_NAME_INDEX] == consumer.name }) {
        logger.info("Registered for group '${consumer.group}' under name '${consumer.name}'")
        redisConn.xgroupCreateconsumer(channel, consumer).let {
            if (it === null || !it) {
                throw Exception("Could not create consumer")
            }
        }
    } else {
        logger.info("Skipping consumer creation, consumer ${consumer.name} already exists for group '${consumer.group}'")
    }
}

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    thread {
        logger.info("Starting")
        val config = CacheConfig.fromEnvironment(environment.config, testing)

        val streamRedis = RedisClient.create(config.streamRedisURI)
        val streamRedisConn = streamRedis.connect().coroutines()
        val cacheRedis = RedisClient.create(config.redis.uri)
        val cacheRedisConn = cacheRedis.connect().coroutines()

        val consumerName = InetAddress.getLocalHost().hostName
        val consumer = Consumer.from(config.redis.invalidationGroup, consumerName)
        val stream = XReadArgs.StreamOffset.lastConsumed(config.redis.invalidationChannel)

        runBlocking(Dispatchers.IO) {
            if (!streamAndGroupExist(streamRedisConn, config.redis)) {
                logger.info("Creating stream '${config.redis.invalidationChannel}' and group '${config.redis.invalidationGroup}'")
                createGroupAndStream(streamRedisConn, config.redis)
            }

            try {
                ensureConsumer(streamRedisConn, config.redis.invalidationChannel, consumer)

                val args = XReadArgs().apply {
                    block(Long.MAX_VALUE)
                }
                val keyManager = KeyManager(config.redis)

                while (true) {
                    streamRedisConn
                        .xreadgroup(consumer, args, stream)
                        .onEach { msg ->
                            Metrics.messages.increment()
                            val resource = msg.body["resource"] ?: throw SerializationException("Message missing key 'resource'")

                            when (val type = msg.body["type"]) {
                                Updated::class.qualifiedName,
                                Deleted::class.qualifiedName -> {
                                    logger.trace { "Processing message of type $type" }
                                    cacheRedisConn.del(keyManager.toEntryKey(resource, "en"))
                                    cacheRedisConn.del(keyManager.toEntryKey(resource, "nl"))
                                    cacheRedisConn.del(keyManager.toEntryKey(resource, "de"))
                                    Metrics.invalidations.increment()
                                }
                                else -> logger.warn { "Ignored message of type '$type'" }
                            }
                        }
                        .catch { e ->
                            when (e) {
                                is Exception -> {
                                    logger.error { e.message }
                                    config.notify(e)
                                }
                                else -> {
                                    logger.error { e.toString() }
                                    config.notify(Exception("Unknown error of type ${e.javaClass.name}: $e"))
                                }
                            }
                        }
                        .launchIn(this)
                }
            } finally {
                logger.warn { "Deleting invalidation consumer '${consumer.name}'" }
                streamRedisConn.xgroupDelconsumer(config.redis.invalidationChannel, consumer)
            }
        }
    }
}
