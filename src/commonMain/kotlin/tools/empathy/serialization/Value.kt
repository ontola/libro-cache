package tools.empathy.serialization

import com.benasher44.uuid.uuid4
import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import tools.empathy.serialization.deep.DeepRecord

@Serializable
sealed class Value(
    @Transient
    val value: String = "",
) {
    @Serializable
    sealed class Id(@Transient open val id: String = "") : Value(id) {
        @Serializable
        @SerialName("id")
        data class Global(
            @SerialName("v")
            override val id: String,
        ) : Id(id) {
            constructor(url: Url) : this(url.toString())
        }

        @Serializable
        @SerialName("lid")
        data class Local(
            @SerialName("v")
            override val id: String = "_:${uuid4()}",
        ) : Id(id)
    }

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
    ) : Value(lexical) {
        constructor(v: kotlin.Int) : this(v.toString(10))
    }

    /**
     * 64-bit signed integer.
     */
    @Serializable
    @SerialName("l")
    data class Long(
        @SerialName("v")
        val lexical: String,
    ) : Value(lexical)

    /**
     * UTF-8 encoded string.
     */
    @Serializable
    @SerialName("s")
    data class Str(
        @SerialName("v")
        val lexical: String,
    ) : Value(lexical)

    /**
     * DateTime formatted as ISO8601 long format.
     */
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

    /**
     * Should not be serialized, but acts as a placeholder for nesting.
     */
    data class NestedRecord(
        @SerialName("v")
        val lexical: String,
    ) : Value(lexical) {
        @Transient
        lateinit var record: DeepRecord
    }
}
