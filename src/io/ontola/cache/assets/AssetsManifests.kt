package io.ontola.cache.assets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AssetsManifests(
    val es5: ResourcesManifest,
    val es6: ResourcesManifest,
)

@Serializable
data class ResourcesManifest(
    val publicFolder: String? = null,
    val defaultBundle: String? = null,
    @SerialName("./sw.js")
    val swJs: String = "/$publicFolder/sw.js",
    @SerialName("main.css")
    val mainCss: String = "/$publicFolder/$defaultBundle.bundle.css",
    @SerialName("main.js")
    val mainJs: String = "/$publicFolder/$defaultBundle.bundle.js",
)
