package io.ontola.libro.metadata

import io.ontola.apex.webmanifest.Manifest
import io.ontola.rdf.hextuples.Hextuple

fun metaDataFromData(
    url: String,
    manifest: Manifest,
    data: List<Hextuple>,
    lang: String,
): MetaData {
    val subjects =
        if (url.endsWith('/'))
            arrayOf(url, url.slice(0..-1))
        else
            arrayOf(url)
    val subjectData = data.filter { subjects.contains(it.subject) }

    val text = findValue(subjectData, MetaDataPredicates.TextPredicates, lang)
    val name = findValue(subjectData, MetaDataPredicates.NamePredicates, lang)

    val coverPhoto = findValue(subjectData, MetaDataPredicates.CoverPredicates, lang)
    val coverPhotoQuads = data.filter { it.subject == coverPhoto }
    val coverURL = findValue(coverPhotoQuads, MetaDataPredicates.CoverUrlPredicates, lang)

    val image = findValue(subjectData, MetaDataPredicates.ImagePredicates, lang)
    val imageQuads = data.filter { it.subject == image }
    val imageURL = findValue(imageQuads, MetaDataPredicates.AvatarUrlPredicates, lang)

    val appIcon = findLargestIcon(manifest.icons ?: emptyArray())
    val strippedText = stripMarkdown(text)

    return MetaData(
        appIcon,
        appName = manifest.shortName,
        name,
        url,
        strippedText,
        coverURL,
        imageURL,
    )
}
