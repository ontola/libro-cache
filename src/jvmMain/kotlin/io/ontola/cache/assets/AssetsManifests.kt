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
    val mainCss: String? = if (defaultBundle != null && publicFolder != null) "/$publicFolder/$defaultBundle.bundle.css" else null,
    @SerialName("main.js")
    val mainJs: String? = if (defaultBundle != null && publicFolder != null) "/$publicFolder/$defaultBundle.bundle.js" else null,
)
