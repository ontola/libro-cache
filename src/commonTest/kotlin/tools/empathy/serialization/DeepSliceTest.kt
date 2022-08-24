package tools.empathy.serialization

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import tools.empathy.serialization.deep.DeepSlice
import tools.empathy.serialization.deep.flatten
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeepSliceTest {
    private val data = """
        {
          "/": {
            "_id": {
              "type": "id",
              "v": "/"
            },
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": {
              "type": "id",
              "v": "https://example.com/Test"
            },
            "http://schema.org/name": {
              "type": "p",
              "v": "Libro",
              "dt": "http://www.w3.org/2001/XMLSchema#string"
            },
            "https://example.com/prop": {
              "_id": {
                "type": "lid",
                "v": "_:b1"
              },
              "http://www.w3.org/1999/02/22-rdf-syntax-ns#type": {
                "type": "id",
                "v": "https://example.com/TestNested"
              }
            }
          }
        }
    """.trimIndent()

    @Test
    fun testNestedParsing() {
        val slice = Json.decodeFromString<DeepSlice>(data)

        assertNotNull(slice)
        assertNotNull(slice["/"])
        assertNull(slice["https://example.com/prop"])
        assert(slice["/"]?.get("https://example.com/prop")?.get(0) is Value.NestedRecord)
    }

    @Test
    fun testFlatten() {
        val slice = Json.decodeFromString<DeepSlice>(data).flatten()

        assertNotNull(slice)
        assertNotNull(slice["/"])
        assertNotNull(slice["_:b1"])
        assertEquals(Value.Id.Local("_:b1"), slice["/"]?.get("https://example.com/prop")?.get(0))
    }
}
