package tools.empathy.libro.server.invalidator

import io.ktor.server.application.Application
import io.lettuce.core.Consumer
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.XGroupCreateArgs
import io.lettuce.core.XReadArgs
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.ontola.transactions.Deleted
import io.ontola.transactions.Updated
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import mu.KotlinLogging
import tools.empathy.libro.server.Metrics
import tools.empathy.libro.server.configuration.LibroConfig
import tools.empathy.libro.server.configuration.RedisConfig
import tools.empathy.libro.server.util.KeyManager
import java.net.InetAddress
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.milliseconds

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

@OptIn(ExperimentalLettuceCoroutinesApi::class)
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val logger = KotlinLogging.logger {}

    thread {
        logger.info("Invalidator started")
        val config = LibroConfig.fromEnvironment(environment.config, testing)

        val streamRedis = RedisClient.create(config.streamRedisURI)
        val streamRedisConn = streamRedis.connect().coroutines()
        val cacheRedis = RedisClient.create(config.redis.uri)
        val cacheRedisConn = cacheRedis.connect().coroutines()

        val consumer = Consumer.from(config.redis.invalidationGroup, InetAddress.getLocalHost().hostName)
        val stream = XReadArgs.StreamOffset.lastConsumed(config.redis.invalidationChannel)

        runBlocking {
            if (!streamAndGroupExist(streamRedisConn, config.redis)) {
                createGroupAndStream(streamRedisConn, config.redis)
            }

            try {
                ensureConsumer(streamRedisConn, config.redis, consumer)

                val args = XReadArgs().apply {
                    block(Long.MAX_VALUE)
                }
                val keyManager = KeyManager(config.redis)

                while (true) {
                    streamRedisConn
                        .xreadgroup(consumer, args, stream)
                        .collect { msg ->
                            Metrics.messages.increment()
                            val resource = msg.body["resource"] ?: throw SerializationException("Message missing key 'resource'")

                            when (val type = msg.body["type"]) {
                                Updated::class.qualifiedName,
                                Deleted::class.qualifiedName -> {
                                    cacheRedisConn.del(keyManager.toEntryKey(resource, "en"))
                                    cacheRedisConn.del(keyManager.toEntryKey(resource, "nl"))
                                    cacheRedisConn.del(keyManager.toEntryKey(resource, "de"))
                                    Metrics.invalidations.increment()
                                }
                                else -> logger.warn("Ignored message with type '$type'")
                            }
                        }

                    delay(100.milliseconds)
                }
            } finally {
                streamRedisConn.xgroupDelconsumer(config.redis.invalidationChannel, consumer)
            }
        }
    }
}
