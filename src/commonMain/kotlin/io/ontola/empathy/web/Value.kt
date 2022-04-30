package io.ontola.empathy.web

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class Value(
    @Transient
    val value: String = "",
) {
    @Serializable
    @SerialName("id")
    data class GlobalId(
        @SerialName("v")
        val id: String,
    ) : Value(id)

    @Serializable
    @SerialName("lid")
    data class LocalId(
        @SerialName("v")
        val id: String,
    ) : Value(id)

    @Serializable
    @SerialName("b")
    data class Bool(
        @SerialName("v")
        val lexical: String,
    ) : Value(lexical)

    /**
     * 32-bit signed integer.
     */
    @Serializable
    @SerialName("i")
    data class Int(
        @SerialName("v")
        val lexical: String,
    ) : Value(lexical)

    /**
     * 64-bit signed integer.
     */
    @Serializable
    @SerialName("l")
    data class Long(
        @SerialName("v")
        val lexical: String,
    ) : Value(lexical)

    @Serializable
    @SerialName("s")
    data class Str(
        @SerialName("v")
        val lexical: String,
    ) : Value(lexical)

    @Serializable
    @SerialName("dt")
    data class DateTime(
        @SerialName("v")
        val lexical: String,
    ) : Value(lexical)

    @Serializable
    @SerialName("p")
    data class Primitive(
        @SerialName("v")
        val lexical: String,
        @SerialName("dt")
        val dataType: String,
    ) : Value(lexical)

    @Serializable
    @SerialName("ls")
    data class LangString(
        @SerialName("v")
        val lexical: String,
        @SerialName("l")
        val lang: String,
    ) : Value(lexical)
}
