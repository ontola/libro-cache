package io.ontola.rdf.hextuples

data class Hextuple(
    val subject: String,
    val predicate: String,
    val value: String,
    val datatype: DataType,
    val language: String,
    val graph: String,
) {
    fun toArray(): Array<String> = arrayOf(
        subject,
        predicate,
        value,
        datatype.value(),
        language,
        graph,
    )

    companion object {
        fun fromArray(data: Array<String>) = Hextuple(
            data[HexPosition.Subject.ordinal],
            data[HexPosition.Predicate.ordinal],
            data[HexPosition.Value.ordinal],
            DataType.fromValue(data[HexPosition.DataType.ordinal]),
            data[HexPosition.Language.ordinal],
            data[HexPosition.Graph.ordinal],
        )
    }
}
