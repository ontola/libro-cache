package io.ontola.cache.document

import com.oracle.truffle.js.lang.JavaScriptLanguage.MODULE_MIME_TYPE
import com.oracle.truffle.js.runtime.JSContextOptions.ESM_EVAL_RETURNS_EXPORTS_NAME
import io.ontola.cache.tenantization.Manifest
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import kotlin.io.path.Path

data class MetaDataArgs(
    val appIcon: String? = "",
    val appName: String? = "",
    val name: String? = "",
    val url: String = "url",
    val text: String? = "",
    val coverURL: String? = "",
    val imageURL: String? = "",
)

data class TagProps(
    val children: String?,
    val content: String?,
    val href: String?,
    val id: String?,
    val itemProp: String?,
    val name: String?,
    val property: String?,
    val rel: String?,
    val type: String,
) {
    companion object {
        fun fromValue(v: Value): TagProps = TagProps(
            children = v.getStringOrNull("children"),
            content = v.getStringOrNull("content"),
            href = v.getStringOrNull("href"),
            id = v.getStringOrNull("id"),
            itemProp = v.getStringOrNull("itemProp"),
            name = v.getStringOrNull("name"),
            property = v.getStringOrNull("property"),
            rel = v.getStringOrNull("rel"),
            type = v.getMember("type").asString(),
        )

        private fun Value.getStringOrNull(property: String): String? {
            val member = getMember(property)

            return if (member?.isString == true) member.asString() else null
        }
    }
}

private fun buildMetaDataContext(): Context = Context
    .newBuilder("js")
    .option(ESM_EVAL_RETURNS_EXPORTS_NAME, "true")
    .allowIO(true)
    .allowAllAccess(true)
    .build()

private fun Context.initMetaData(): Value {
    val metaDataFile = Path("build/client/metaData.js").toFile()
    val metaDataSource = Source
        .newBuilder("js", metaDataFile)
        .mimeType(MODULE_MIME_TYPE)
        .build()

    return eval(metaDataSource)
}

fun contentMetaTags(args: MetaDataArgs): List<TagProps> = buildMetaDataContext().use { context ->
    val metaData = context.initMetaData()

    val getMetaTags = metaData.getMember("getMetaTags")
    assert(getMetaTags.canExecute())
    val test = ProxyObject.fromMap(
        mapOf(
            "appIcon" to args.appIcon,
            "appName" to args.appName,
            "name" to args.name,
            "url" to args.url,
            "text" to args.text,
            "coverURL" to args.coverURL,
            "imageURL" to args.imageURL,
        )
    )
    val result = getMetaTags.execute(test)

    (0 until result.arraySize)
        .map { result.getMember(it.toString()) }
        .map { TagProps.fromValue(it) }
}

fun renderedMetaTags(href: String, manifest: Manifest, data: String): String = buildMetaDataContext().use { context ->
    val metaData = context.initMetaData()

    val prerenderMetaTags = metaData.getMember("prerenderMetaTags")
    assert(prerenderMetaTags.canExecute())

    prerenderMetaTags.execute(href, manifest, data).asString()
}
