package tools.empathy.vocabularies

object LinkLib : Vocab {
    override val vocab: String = "http://purl.org/link-lib/"

    val Accept by Term()
    val ErrorResource by Term()
    val ErrorResponseClass = "${vocab}ErrorResponse"
    val LoadingResourceClass = "${vocab}LoadingResource"

    /* properties */
    val actionBody by Term()
    val blob by Term()
    val dataSubject by Term()
    val errorResponse by Term()
    val forceRender by Term()
    val loadingResource by Term()
    val meta by Term()
    val view by Term()
}
