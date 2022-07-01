package io.ontola.empathy.web

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private val json = Json {
    prettyPrint = true
}

class RecordSerializerTest {
    private val homeId = Value.Id.Global("https://example.com/")
    private val name = Value.Id.Global("http://schema.org/name")
    private val homeNameNl = Value.LangString("Example thuis", "nl")
    private val homeNameEn = Value.LangString("Example home", "en")
    private val dateCreated = Value.Id.Global("http://schema.org/dateCreated")
    private val homeCreated = Value.Primitive("2020-01-01T00:00:00Z", "http://www.w3.org/2001/XMLSchema#date")
    private val homeSerialized = """
        {
            "_id": {
                "type": "id",
                "v": "${homeId.value}"
            },
            "${name.value}": [
                {
                    "type": "ls",
                    "v": "${homeNameNl.value}",
                    "l": "nl"
                },
                {
                    "type": "ls",
                    "v": "${homeNameEn.value}",
                    "l": "en"
                }
            ],
            "${dateCreated.value}": {
                "type": "p",
                "v": "${homeCreated.value}",
                "dt": "${homeCreated.dataType}"
            }
        }
    """.trimIndent()
    private val localIdSerialized = """
        {
            "_id": {
                "type": "lid",
                "v": "_:abc"
            }
        }
    """.trimIndent()

    @Test
    fun testDeserialization() {
        val parsed = Json.decodeFromString<Record>(homeSerialized)

        assertEquals(homeId, parsed.id)
        assertEquals(2, parsed.fields.size)

        val nameField = parsed.fields[name.value]
        assertNotNull(nameField)
        assertEquals(2, nameField.size)
        assertEquals(homeNameNl, nameField[0])
        assertEquals(homeNameEn, nameField[1])

        val dateCreatedField = parsed.fields[dateCreated.value]
        assertNotNull(dateCreatedField)
        assertEquals(1, dateCreatedField.size)
        assertEquals(homeCreated, dateCreatedField.first())
    }

    @Test
    fun testSerialization() {
        val record = Record(
            homeId,
            mutableMapOf(
                name.value to listOf(
                    homeNameNl,
                    homeNameEn,
                ),
                dateCreated.value to listOf(homeCreated)
            )
        )

        val serialized = json.encodeToString(record)

        assertEquals(homeSerialized, serialized)
    }

    @Test
    fun testLocalIdSerialization() {
        val record = Record(
            Value.Id.Local("_:abc"),
            mutableMapOf()
        )

        val serialized = json.encodeToString(record)

        assertEquals(localIdSerialized, serialized)
    }

    private fun testProp(value: Value, serializedProp: String) {
        val serialized = """
        {
            "_id": {
                "type": "id",
                "v": "${homeId.value}"
            },
            "prop": $serializedProp
        }
        """.trimIndent()
        val record = Record(
            homeId,
            mutableMapOf(
                "prop" to listOf(value)
            )
        )

        val result = json.encodeToString(record)
        assertEquals(serialized, result)

        val deserialized = json.decodeFromString<Record>(serialized)
        assertEquals(record["prop"]?.get(0), deserialized["prop"]?.get(0))
    }

    @Test
    fun testBooleanSerialisation() = testProp(
        Value.Bool("true"),
        """
        {
                        "type": "b",
                        "v": "true"
                    }
        """.trimIndent(),
    )

    @Test
    fun testStringSerialisation() = testProp(
        Value.Str("Beautiful"),
        """
        {
                        "type": "s",
                        "v": "Beautiful"
                    }
        """.trimIndent(),
    )

    @Test
    fun testDateTimeSerialisation() = testProp(
        Value.DateTime("2022-04-12T13:44:31Z"),
        """
        {
                        "type": "dt",
                        "v": "2022-04-12T13:44:31Z"
                    }
        """.trimIndent(),
    )
}
