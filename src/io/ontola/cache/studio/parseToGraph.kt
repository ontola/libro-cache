package io.ontola.cache.studio

import com.oracle.truffle.js.lang.JavaScriptLanguage
import com.oracle.truffle.js.runtime.JSContextOptions
import io.ktor.http.Url
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import kotlin.io.path.Path


private fun buildMetaDataContext(): Context = Context
    .newBuilder("js")
    .option(JSContextOptions.ESM_EVAL_RETURNS_EXPORTS_NAME, "true")
    .allowIO(true)
    .allowAllAccess(true)
    .build()

private fun Context.init(): Value {
    val test = """
        __TEST__ = undefined;
        __CLIENT__ = undefined;
        Intl = {};
        File = undefined;
    """.trimIndent()
    val globals = Source.newBuilder("js", test, "globals.js").build()
    eval(globals)

    val parseToGraphFile = Path("build/client/parseToGraph.js").toFile()
    val metaDataSource = Source
        .newBuilder("js", parseToGraphFile)
        .mimeType(JavaScriptLanguage.MODULE_MIME_TYPE)
        .build()

    return eval(metaDataSource)
}

object ParseToGraph {
    private val context = buildMetaDataContext()
    private val metaData = context.init()
    val sourceToHextuples: Value = metaData.getMember("sourceToHextuples")

    init {
        assert(sourceToHextuples.canExecute())
    }
}

fun sourceToHextuples(source: String, uri: Url, origin: String? = null): String {
    return ParseToGraph.sourceToHextuples.execute(source, uri.toString(), origin).asString()
}
