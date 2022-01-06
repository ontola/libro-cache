package io.ontola.cache.plugins

import mu.KotlinLogging
import java.io.File
import java.nio.charset.Charset

private val logger = KotlinLogging.logger {}

fun getVersion(name: String): String? {
    return try {
        val file = File("${name}_version.txt")
        if (file.exists()) {
            file
                .readText(Charsets.UTF_8)
                .ifBlank { fromLocalGitRepo(name) }
        } else {
            fromLocalGitRepo(name)
        }
    } catch (e: Exception) {
        println(e)
        null
    }
}

object Versions {
    val ClientVersion = getVersion("client")?.trimEnd('\n')
    val ServerVersion = getVersion("server")?.trimEnd('\n')

    fun print() {
        logger.info("Server version: $ServerVersion")
        logger.info("Client version: $ClientVersion")
    }
}

fun fromLocalGitRepo(name: String): String? {
    if (name != "server") {
        return null
    }

    val cmd = Runtime.getRuntime().exec("git describe --always")
    cmd.waitFor()

    return cmd
        .inputStream
        .readAllBytes()
        .toString(Charset.defaultCharset())
}
