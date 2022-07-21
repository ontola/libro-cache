package io.ontola.cache.bundle

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Bundles(
    val es5: BundleManifest,
    val es6: BundleManifest,
)

@Serializable
data class BundleManifest(
    val publicFolder: String? = null,
    val defaultBundle: String? = null,
    @SerialName("./sw.js")
    val swJs: String = "/$publicFolder/sw.js",
    @SerialName("main.css")
    val mainCss: String = "/$publicFolder/$defaultBundle.bundle.css",
    @SerialName("main.js")
    val mainJs: String = "/$publicFolder/$defaultBundle.bundle.js",
)
