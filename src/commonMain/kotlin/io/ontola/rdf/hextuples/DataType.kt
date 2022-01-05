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
            "globalId" -> GlobalId()
            "localId" -> LocalId()
            else -> Literal(dataType)
        }
    }
}
