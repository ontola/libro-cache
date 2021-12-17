package io.ontola.libro.metadata

private const val ontolaNS = "https://ns.ontola.io/core#"
private const val schemaNS = "http://schema.org/"
private const val rdfsNS = "http://www.w3.org/2000/01/rdf-schema#"
private const val dboNS = "http://dbpedia.org/ontology/"

object MetaDataPredicates {
    val CoverPredicates = arrayOf(
        "${ontolaNS}coverPhoto",
    )
    val CoverUrlPredicates = arrayOf(
        "${ontolaNS}imgUrl1500x2000",
    )
    val AvatarUrlPredicates = arrayOf(
        "${ontolaNS}imgUrl256x256",
    )
    val ImagePredicates = arrayOf(
        "${schemaNS}image",
    )
    val NamePredicates = arrayOf(
        "${schemaNS}name",
        "${rdfsNS}label",
    )
    val TextPredicates = arrayOf(
        "${dboNS}abstract",
        "${schemaNS}description",
        "${schemaNS}text",
    )
}
