package io.ontola.libro.metadata

import io.ktor.http.Url
import io.ontola.apex.webmanifest.Manifest
import io.ontola.empathy.web.DataSlice
import io.ontola.empathy.web.Record
import io.ontola.empathy.web.Value
import io.ontola.util.absolutize

fun metaDataFromData(
    url: Url,
    manifest: Manifest,
    data: DataSlice,
    lang: String,
): MetaData {
    val subject = manifest.ontola.websiteIRI.absolutize(url)
    val subjectData =
        if (subject.endsWith('/'))
            merge(data[subject], data[subject.slice(0..-1)])
        else
            data[subject]

    if (subjectData == null)
        return MetaData(appName = manifest.shortName, url = url)

    val websiteIRI = manifest.ontola.websiteIRI

    val text = findValue(subjectData, MetaDataPredicates.TextPredicates, lang, websiteIRI)
    val name = findValue(subjectData, MetaDataPredicates.NamePredicates, lang, websiteIRI)

    val coverPhoto = findValue(subjectData, MetaDataPredicates.CoverPredicates, lang, websiteIRI)
    val coverPhotoQuads = data[coverPhoto]
    val coverURL = findValue(coverPhotoQuads, MetaDataPredicates.CoverUrlPredicates, lang, websiteIRI)

    val image = findValue(subjectData, MetaDataPredicates.ImagePredicates, lang, websiteIRI)
    val imageQuads = data[image]
    val imageURL = findValue(imageQuads, MetaDataPredicates.AvatarUrlPredicates, lang, websiteIRI)

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

private fun merge(a: Record?, b: Record?): Record? {
    val mergedFields = HashMap<String, List<Value>>().apply {
        if (a == null && b == null)
            return null

        if (a != null)
            for ((key, value) in a.entries) {
                this[key] = value
            }

        if (b != null)
            for ((key, value) in b.entries) {
                val existing = this[key]
                if (existing == null) {
                    this[key] = value
                } else {
                    this[key] = existing + value
                }
            }
    }

    return Record(a?.id ?: b!!.id, mergedFields)
}
