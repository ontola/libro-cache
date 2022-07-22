package tools.empathy.vocabularies

import tools.empathy.serialization.Value

object Schema : Vocab {
    override val vocab = "http://schema.org/"

    val Action by Term()
    val ActionStatusType by Term()
    val Blog by Term()
    val BlogPosting by Term()
    val CommentClass = "${vocab}Comment"
    val CreateAction by Term()
    val CreativeWork by Term()
    val DataDownload by Term()
    val EntryPoint by Term()
    val FollowAction by Term()
    val ImageObject by Term()
    val MediaObject by Term()
    val Person by Term()
    val Place by Term()
    val Thing by Term()
    val UpdateAction by Term()
    val VideoObject by Term()
    val WebPage by Term()
    val WebSite by Term()

    val actionStatus by Term()
    val birthDate by Term()
    val blogPosts by Term()
    val breadcrumb by Term()
    val contentType by Term()
    val contentUrl by Term()
    val creator by Term()
    val dateCreated by Term()
    val dateDeleted by Term()
    val dateIssued by Term()
    val dateModified by Term()
    val datePosted by Term()
    val datePublished by Term()
    val dateRead by Term()
    val dateReceived by Term()
    val dateSent by Term()
    val downloadUrl by Term()
    val encodingFormat by Term()
    val httpMethod by Term()
    val item by Term()
    val language = "${vocab}language"
    val map by Term()
    val maps by Term()
    val members by Term()
    val name by Term()
    val postalCode by Term()
    val potentialAction by Term()
    val publication by Term()
    val publishedOn by Term()
    val publisher by Term()
    val startDate by Term()
    val target by Term()
    val targetUrl by Term()
    val text by Term()
    val thumbnail by Term()
    val thumbnailUrl by Term()
    val uploadDate by Term()
    val url = Value.Id.Global("${vocab}url")
    val urlTemplate by Term()
    val actionApplication by Term()
    val actor by Term()
    val dataset by Term()
    val description by Term()
    val event by Term()
    val image by Term()
    val result by Term()
    val member by Term()
    val isPartOf by Term()
    val `object` by Term()
    val location by Term()
}
