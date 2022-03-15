package io.ontola.empathy.web

import io.ontola.rdf.hextuples.DataType
import io.ontola.rdf.hextuples.Hextuple
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val slice = """
{
   "https://argu.co/info":{
      "_id":{
         "type":"id",
         "v":"https://argu.co/info"
      },
      "http://schema.org/name":[
         {
            "type":"p",
            "v":"Argu - Beslis samen beter",
            "dt":"http://www.w3.org/2001/XMLSchema#string"
         }
      ],
      "http://schema.org/description":[
         {
            "type":"p",
            "v":"Argu is een gebruiksklaar participatieplatform voor iedere organisatie. Betrek meer burgers, bouw een community en beslis beter.  Vraag vrijblijvend een demo aan.",
            "dt":"http://www.w3.org/2001/XMLSchema#string"
         }
      ],
      "https://ns.ontola.io/core#coverPhoto":[
         {
            "type":"id",
            "v":"https://argu.co/info#CoverImage"
         }
      ],
      "https://argu.co/ns/sales#header":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://argu.co/ns/sales#header>"
         }
      ],
      "https://argu.co/ns/sales#callToActionBlock":[
         {
            "type":"id",
            "v":"https://argu.co/info#CTABlock"
         }
      ],
      "https://ns.ontola.io/core#navigationsMenu":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>"
         }
      ],
      "https://argu.co/ns/sales#showcase":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://argu.co/ns/sales#showcase>"
         }
      ],
      "https://argu.co/ns/sales#cases":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://argu.co/ns/sales#cases>"
         }
      ],
      "https://argu.co/ns/sales#propositions":[
         {
            "type":"id",
            "v":"https://argu.co/info#propositions"
         }
      ],
      "https://argu.co/ns/sales#duoBlock":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://argu.co/ns/sales#duoBlock>"
         }
      ],
      "https://argu.co/ns/sales#blogs":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://argu.co/ns/sales#blogs>"
         }
      ]
   },
   "https://argu.co/info.<https://argu.co/ns/sales#header>":{
      "_id":{
         "type":"id",
         "v":"https://argu.co/info.<https://argu.co/ns/sales#header>"
      },
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#type":[
         {
            "type":"id",
            "v":"https://argu.co/ns/sales#Header"
         }
      ],
      "http://schema.org/name":[
         {
            "type":"p",
            "v":"Beslis samen beter",
            "dt":"http://www.w3.org/2001/XMLSchema#string"
         }
      ],
      "http://schema.org/text":[
         {
            "type":"p",
            "v":"Wij helpen jou om je doelgroep te betrekken. Simpel, interactief en toegankelijk.",
            "dt":"http://www.w3.org/2001/XMLSchema#string"
         }
      ],
      "https://argu.co/ns/sales#buttonLink":[
         {
            "type":"id",
            "v":"https://calendly.com/argu_co/online-demo"
         }
      ],
      "https://argu.co/ns/sales#buttonText":[
         {
            "type":"p",
            "v":"Plan een demo",
            "dt":"http://www.w3.org/2001/XMLSchema#string"
         }
      ],
      "https://argu.co/ns/sales#backgroundImage":[
         {
            "type":"id",
            "v":"https://dptr8y9slmfgv.cloudfront.net/sales/images/header.svg"
         }
      ],
      "https://argu.co/ns/sales#backgroundImageMobile":[
         {
            "type":"id",
            "v":"https://dptr8y9slmfgv.cloudfront.net/sales/images/header_mobile.svg"
         }
      ],
      "https://argu.co/ns/sales#backgroundImageXL":[
         {
            "type":"id",
            "v":"https://dptr8y9slmfgv.cloudfront.net/sales/images/header_xl.svg"
         }
      ]
   },
   "https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>":{
      "_id":{
         "type":"id",
         "v":"https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>"
      },
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#type":[
         {
            "type":"id",
            "v":"https://ns.ontola.io/core#MenuItem"
         }
      ],
      "https://argu.co/ns/sales#callToAction":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://argu.co/ns/sales#callToAction>"
         }
      ],
      "https://ns.ontola.io/core#menuItems":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>"
         }
      ]
   },
   "https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://argu.co/ns/sales#callToAction>":{
      "_id":{
         "type":"id",
         "v":"https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://argu.co/ns/sales#callToAction>"
      },
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#type":[
         {
            "type":"id",
            "v":"https://argu.co/ns/sales#CallToActionButton"
         }
      ],
      "http://schema.org/text":[
         {
            "type":"p",
            "v":"Plan een demo",
            "dt":"http://www.w3.org/2001/XMLSchema#string"
         }
      ],
      "https://ns.ontola.io/core#href":[
         {
            "type":"id",
            "v":"https://calendly.com/argu_co/online-demo"
         }
      ]
   },
   "https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>":{
      "_id":{
         "type":"id",
         "v":"https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>"
      },
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#type":[
         {
            "type":"id",
            "v":"http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq"
         }
      ],
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#_0":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_0>"
         }
      ],
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#_1":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_1>"
         }
      ],
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#_2":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_2>"
         }
      ],
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#_3":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_3>"
         }
      ],
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#_4":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_4>"
         }
      ],
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#_5":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_5>"
         }
      ],
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#_6":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_6>"
         }
      ],
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#_7":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_7>"
         }
      ],
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#_8":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_8>"
         }
      ],
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#_9":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_9>"
         }
      ]
   },
   "https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_0>":{
      "_id":{
         "type":"id",
         "v":"https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_0>"
      },
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#type":[
         {
            "type":"id",
            "v":"https://ns.ontola.io/core#MenuItem"
         }
      ],
      "http://schema.org/image":[
         {
            "type":"lid",
            "v":"_:https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_0>.<http://schema.org/image>"
         }
      ],
      "http://schema.org/isPartOf":[
         {
            "type":"id",
            "v":"https://argu.co/info"
         }
      ],
      "http://schema.org/name":[
         {
            "type":"p",
            "v":"Argu",
            "dt":"http://www.w3.org/2001/XMLSchema#string"
         }
      ],
      "https://ns.ontola.io/core#href":[
         {
            "type":"id",
            "v":"https://argu.co/info"
         }
      ]
   },
   "https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_0>.<http://schema.org/image>":{
      "_id":{
         "type":"id",
         "v":"https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_0>.<http://schema.org/image>"
      },
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#type":[
         {
            "type":"id",
            "v":"https://ns.ontola.io/core#PictureSet"
         }
      ],
      "https://ns.ontola.io/core#ariaLabel":[
         {
            "type":"p",
            "v":"Logo",
            "dt":"http://www.w3.org/2001/XMLSchema#string"
         }
      ],
      "https://ns.ontola.io/core#format/svg":[
         {
            "type":"id",
            "v":"https://dptr8y9slmfgv.cloudfront.net/sales/images/argu-logo.svg"
         }
      ],
      "https://ns.ontola.io/core#format/png":[
         {
            "type":"id",
            "v":"https://dptr8y9slmfgv.cloudfront.net/sales/images/argu-logo.png"
         }
      ]
   },
   "https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_1>":{
      "_id":{
         "type":"id",
         "v":"https://argu.co/info.<https://ns.ontola.io/core#navigationsMenu>.<https://ns.ontola.io/core#menuItems>.<http://www.w3.org/1999/02/22-rdf-syntax-ns#_1>"
      },
      "http://www.w3.org/1999/02/22-rdf-syntax-ns#type":[
         {
            "type":"id",
            "v":"https://ns.ontola.io/core#MenuItem"
         }
      ],
      "http://schema.org/name":[
         {
            "type":"p",
            "v":"Functionaliteiten",
            "dt":"http://www.w3.org/2001/XMLSchema#string"
         }
      ],
      "https://ns.ontola.io/core#href":[
         {
            "type":"id",
            "v":"https://argu.co/info/functionaliteiten"
         }
      ]
   }
}
""".trimIndent()

private val hex = """
[
    [
        "https://argu.co/info",
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#type",
        "https://argu.co/ns/sales#HomePage",
        "globalId",
        "",
        "http://purl.org/linked-delta/supplant"
    ],
    [
        "https://argu.co/info",
        "http://schema.org/name",
        "Argu - Beslis samen beter",
        "http://www.w3.org/2001/XMLSchema#string",
        "",
        "http://purl.org/linked-delta/supplant"
    ],
    [
        "https://argu.co/info",
        "http://schema.org/description",
        "Argu is een gebruiksklaar participatieplatform voor iedere organisatie. Betrek meer burgers, bouw een community en beslis beter.  Vraag vrijblijvend een demo aan.",
        "http://www.w3.org/2001/XMLSchema#string",
        "",
        "http://purl.org/linked-delta/supplant"
    ],
    [
        "https://argu.co/info",
        "https://ns.ontola.io/core#coverPhoto",
        "https://argu.co/info#CoverImage",
        "globalId",
        "",
        "http://purl.org/linked-delta/supplant"
    ]
]
""".trimIndent()

class SeedTest {
    @Test
    fun testToSliceWorksOnEmptyList() {
        val result = emptyList<Hextuple>().toSlice()

        assertTrue(result.isEmpty())
    }

    @Test
    fun testToSlice() {
        val data = listOf(
            Hextuple("https://argu.co/info", "https://ns.ontola.io/core#coverPhoto", "https://argu.co/info#CoverImage", DataType.GlobalId(), "", supplantGraph),
        )
        val result = data.toSlice()

        assertEquals(
            Value.GlobalId("https://argu.co/info#CoverImage"),
            result["https://argu.co/info"]?.get("https://ns.ontola.io/core#coverPhoto")?.first()!! as Value.GlobalId,
        )
    }

    @Test
    fun testSliceParsing() {
        val result = Json.decodeFromString<DataSlice>(slice)

        assertTrue(result.isNotEmpty())
        assertEquals(
            Value.GlobalId("https://argu.co/info#CoverImage"),
            result["https://argu.co/info"]?.get("https://ns.ontola.io/core#coverPhoto")?.first()!! as Value.GlobalId,
        )
    }

    @Test
    fun test() {
        val data = Json.decodeFromString<List<Hextuple>>(hex)

        assertEquals(data.size, 4)

        val seed = data.toSlice()

        assertEquals(
            Value.GlobalId("https://argu.co/info#CoverImage"),
            seed["https://argu.co/info"]?.get("https://ns.ontola.io/core#coverPhoto")?.first()!! as Value.GlobalId,
        )
    }
}
