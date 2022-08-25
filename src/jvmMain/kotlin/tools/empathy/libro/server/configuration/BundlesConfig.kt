package tools.empathy.libro.server.configuration

data class BundlesConfig(
    val es6ManifestLocation: String = "./resources/client/manifest.module.json",
    val es5ManifestLocation: String = "./resources/client/manifest.legacy.json",
    val publicFolder: String,
    val defaultBundle: String,
)
