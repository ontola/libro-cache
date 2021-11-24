package io.ontola.cache.util

import io.ontola.cache.document.AssetsManifests
import io.ontola.cache.document.ResourcesManifest
import io.ontola.cache.plugins.CacheConfig
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.io.path.Path

fun loadAssetsManifests(config: CacheConfig) = if (!config.isDev && !config.isTesting) {
    val es5Manifest = Files.readString(Path(config.assets.es5ManifestLocation))
    val es6Manifest = Files.readString(Path(config.assets.es6ManifestLocation))
    val parser = Json {
        ignoreUnknownKeys = true
    }
    AssetsManifests(
        es5 = parser.decodeFromString(es5Manifest),
        es6 = parser.decodeFromString(es6Manifest),
    )
} else {
    AssetsManifests(
        ResourcesManifest(
            publicFolder = config.assets.publicFolder,
            defaultBundle = config.assets.defaultBundle,
        ),
        ResourcesManifest(
            publicFolder = config.assets.publicFolder,
            defaultBundle = config.assets.defaultBundle,
        )
    )
}
