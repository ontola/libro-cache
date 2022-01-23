package io.ontola.rdf.hextuples

interface Valuable {
    fun value(): String
}

sealed class DataType(private val datatype: String) : Valuable {
    override fun value(): String = datatype

    class LocalId(value: String = "localId") : DataType(value)
    class GlobalId(value: String = "globalId") : DataType(value)
    class Literal(value: String) : DataType(value)

    companion object {
        fun fromValue(dataType: String): DataType = when (dataType) {
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode" -> GlobalId()
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#blankNode" -> LocalId()
            else -> Literal(dataType)
        }
    }
}
