package tools.empathy.model

import tools.empathy.serialization.Value

/**
 * A map of language values.
 */
class LangMap : MutableMap<String, Value.LangString> by mutableMapOf()

class LangMapBuilder {
    var de: String? = null
    var en: String? = null
    var nl: String? = null

    fun build(): LangMap {
        return LangMap().apply {
            this@LangMapBuilder.de?.let { this["de"] = Value.LangString(it, "de") }
            this@LangMapBuilder.en?.let { this["en"] = Value.LangString(it, "en") }
            this@LangMapBuilder.nl?.let { this["nl"] = Value.LangString(it, "nl") }
        }
    }
}

fun langMap(init: LangMapBuilder.() -> Unit): LangMap {
    val builder = LangMapBuilder()
    builder.init()
    return builder.build()
}
