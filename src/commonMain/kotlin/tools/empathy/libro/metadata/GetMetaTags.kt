package tools.empathy.libro.metadata

import tools.empathy.libro.webmanifest.Icon

/**
 * Converts a [MetaData] object to HTML headers used by search engines and social media.
 */
fun getMetaTags(data: MetaData): List<TagProps> = buildList {
    val title = arrayOf(data.name, data.appName)
        .filterNotNull()
        .joinToString(" | ")

    add(
        TagProps(
            children = data.name?.ifBlank { null } ?: data.appName,
            type = "title",
        )
    )
    add(
        TagProps(
            href = data.url,
            itemProp = "url",
            rel = "canonical",
            type = "link",
        )
    )
    add(
        TagProps(
            href = data.url,
            itemProp = "url",
            rel = "canonical",
            type = "link",
        )
    )
    add(
        TagProps(
            content = data.url.toString(),
            property = "og:url",
            type = "meta",
        )
    )
    add(
        TagProps(
            content = title,
            property = "og:title",
            type = "meta",
        )
    )
    add(
        TagProps(
            content = title,
            name = "twitter:title",
            type = "meta",
        )
    )
    add(
        TagProps(
            content = if (data.coverURL.isNullOrBlank()) "summary" else "summary_large_image",
            name = "twitter:card",
            type = "meta",
        )
    )

    val img = data.coverURL ?: data.imageURL ?: data.appIcon

    if (img != null) {
        add(
            TagProps(
                content = img,
                id = "og:image",
                property = "og:image",
                type = "meta",
            )
        )
        add(
            TagProps(
                content = img,
                name = "twitter:image",
                type = "meta",
            )
        )
    }

    if (data.text != null) {
        add(
            TagProps(
                content = data.text,
                id = "og:description",
                property = "og:description",
                type = "meta",
            )
        )
        add(
            TagProps(
                content = data.text,
                name = "twitter:description",
                type = "meta",
            )
        )
        add(
            TagProps(
                content = data.text,
                id = "description",
                name = "description",
                property = "description",
                type = "meta",
            )
        )
    }
}

internal fun findLargestIcon(icons: Array<Icon>): String? = appIconSizePriority
    .firstNotNullOfOrNull { size ->
        icons.find { icon -> icon.sizes == size }
    }
    ?.src
