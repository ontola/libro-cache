package tools.empathy.libro.server.bundle

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import tools.empathy.libro.server.plugins.CacheConfig
import java.nio.file.Files
import kotlin.io.path.Path

internal fun loadBundleManifests(config: CacheConfig) = if (!config.isDev && !config.isTesting) {
    val es5Manifest = Files.readString(Path(config.bundles.es5ManifestLocation))
    val es6Manifest = Files.readString(Path(config.bundles.es6ManifestLocation))
    val parser = Json {
        ignoreUnknownKeys = true
    }
    Bundles(
        es5 = parser.decodeFromString(es5Manifest),
        es6 = parser.decodeFromString(es6Manifest),
    )
} else {
    Bundles(
        BundleManifest(
            publicFolder = config.bundles.publicFolder,
            defaultBundle = config.bundles.defaultBundle,
        ),
        BundleManifest(
            publicFolder = config.bundles.publicFolder,
            defaultBundle = config.bundles.defaultBundle,
        )
    )
}
