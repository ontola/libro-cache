package tools.empathy.libro.server.configuration

data class BundlesConfig(
    val es6ManifestLocation: String = "./assets/manifest.module.json",
    val es5ManifestLocation: String = "./assets/manifest.legacy.json",
    val publicFolder: String,
    val defaultBundle: String,
)
