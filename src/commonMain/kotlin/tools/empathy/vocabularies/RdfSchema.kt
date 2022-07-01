package tools.empathy.vocabularies

object RdfSchema : Vocab {
    override val vocab: String = "http://www.w3.org/2000/01/rdf-schema#"

    val Resource by Term()
    val Class by Term()
    val Literal by Term()
    val Container by Term()
    val ContainerMembershipProperty by Term()
    val Datatype by Term()
    val subClassOf by Term()
    val subPropertyOf by Term()
    val comment by Term()
    val label by Term()
    val domain by Term()
    val range by Term()
    val seeAlso by Term()
    val isDefinedBy by Term()
    val member by Term()
}
