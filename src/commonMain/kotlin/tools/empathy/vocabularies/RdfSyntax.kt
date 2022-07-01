package tools.empathy.vocabularies

object RdfSyntax : Vocab {
    override val vocab: String = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"

    val HTML by Term()
    val langString by Term()
    val PlainLiteral by Term()
    val Property by Term()
    val Statement by Term()
    val Bag by Term()
    val Seq by Term()
    val Alt by Term()
    val List by Term()
    val nil by Term()
    val XMLLiteral by Term()
    val type by Term()
    val subject by Term()
    val predicate by Term()
    val `object` by Term()
    val value by Term()
    val first by Term()
    val rest by Term()
}
