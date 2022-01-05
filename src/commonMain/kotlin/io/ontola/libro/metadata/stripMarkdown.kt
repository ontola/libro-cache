package io.ontola.libro.metadata

import kotlin.js.JsExport

val header = Regex("\\n={2,}")
val tildeFencedCodeblocks = Regex("~{3}.*\\n")
val backtickFencedCodeblocks = Regex("`{3}.*\\n")
val strikethrough = Regex("~~")
val htmlTags = Regex("<[^>]*>")

val setextHeaders = Regex("^[=-]{2,}\\s*$")
val footnotes = Regex("\\[\\^.+?\\](: .*?$)?")
val footnotesExtra = Regex("\\s{0,2}\\[.*?\\]: .*?$")
val images = Regex("!\\[(.*?)\\][\\[(].*?[\\])]")
val inlineLinks = Regex("\\[(.*?)\\][\\[(].*?[\\])]")
val blockquotes = Regex("^\\s{0,3}>\\s?")
val referenceStyleLinks = Regex("^\\s{1,2}\\[(.*?)]: (\\S+)( \".*?\")?\\s*$")
// Differs from the original implementation, see https://github.com/stiang/remove-markdown/issues/52
val atxStyleHeaders = Regex("^(#{1,6}) ", RegexOption.MULTILINE)
val emphasis = Regex("([*_]{1,3})(\\S.*?\\S?)\\1")
val codeBlocks = Regex("(`{3,})(.*?)\\1")
val inlineCode = Regex("`(.+?)`", RegexOption.MULTILINE)
val multiNewlines = Regex("\n{2,}")
val newlineNormalize = Regex("[\r\n]")

// Modified from https://github.com/stiang/remove-markdown/blob/master/index.js
@JsExport
fun stripMarkdown(value: String?): String {
    if (value == null) {
        return ""
    }

    return value
        .replace(header, "\n")
        .replace(tildeFencedCodeblocks, "")
        .replace(strikethrough, "")
        .replace(backtickFencedCodeblocks, "")
        .replace(htmlTags, "")
        .replace(setextHeaders, "")
        .replace(footnotes, "")
        .replace(footnotesExtra, "")
        .replace(images, "$1")
        .replace(inlineLinks, "$1")
        .replace(blockquotes, "")
        .replace(referenceStyleLinks, "")
        .replace(atxStyleHeaders, "")
        .replace(emphasis, "$2")
        .replace(emphasis, "$2")
        .replace(codeBlocks, "$2")
        .replace(inlineCode, "$1")
        .replace(multiNewlines, "\n\n")
        .replace(newlineNormalize, " ");
}
