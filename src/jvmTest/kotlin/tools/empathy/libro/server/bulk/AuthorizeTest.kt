package tools.empathy.libro.server.bulk

import kotlin.test.Test
import kotlin.test.assertEquals

class AuthorizeTest {
    @Test
    fun testScopeBlankNodes() {
        val source = """
            ["https://argu.localdev/argu/forms/linked_rails/auth/sessions","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","https://ns.ontola.io/form#Form","http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode","","http://purl.org/linked-delta/supplant"]
            ["https://argu.localdev/argu/forms/linked_rails/auth/sessions","https://ns.ontola.io/form#pages","_:g670580","http://www.w3.org/1999/02/22-rdf-syntax-ns#blankNode","","http://purl.org/linked-delta/supplant"]
            ["_:g670580","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq","http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode","","http://purl.org/linked-delta/supplant"]
            ["_:g670580","http://www.w3.org/1999/02/22-rdf-syntax-ns#_0","_:g647500","http://www.w3.org/1999/02/22-rdf-syntax-ns#blankNode","","http://purl.org/linked-delta/supplant"]
            ["_:g647540","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","https://ns.ontola.io/form#EmailInput","http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode","","http://purl.org/linked-delta/supplant"]
            ["_:g647540","http://schema.org/name","Email","http://www.w3.org/2001/XMLSchema#string","","http://purl.org/linked-delta/supplant"]
            ["_:g647540","https://ns.ontola.io/core#helperText","","http://www.w3.org/2001/XMLSchema#string","","http://purl.org/linked-delta/supplant"]
            ["_:g647540","https://ns.ontola.io/form#placeholder","email@voorbeeld.nl","http://www.w3.org/2001/XMLSchema#string","","http://purl.org/linked-delta/supplant"]
            ["_:g647540","http://www.w3.org/ns/shacl#datatype","http://www.w3.org/2001/XMLSchema#string","http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode","","http://purl.org/linked-delta/supplant"]
            ["_:g647540","http://www.w3.org/ns/shacl#maxCount","1","http://www.w3.org/2001/XMLSchema#integer","","http://purl.org/linked-delta/supplant"]
            ["_:g647540","http://www.w3.org/ns/shacl#minCount","1","http://www.w3.org/2001/XMLSchema#integer","","http://purl.org/linked-delta/supplant"]
            ["_:g647540","http://www.w3.org/ns/shacl#pattern","\\A(?:[^\\x00-\\x20\\x22\\x28\\x29\\x2c\\x2e\\x3a-\\x3c\\x3e\\x40\\x5b-\\x5d\\x7f-\\u00ff]+|\\x22(?:[^\\x0d\\x22\\x5c\\u0080-\\u00ff]|\\x5c[\\x00-\\x7f])*\\x22)(?:\\x2e(?:[^\\x00-\\x20\\x22\\x28\\x29\\x2c\\x2e\\x3a-\\x3c\\x3e\\x40\\x5b-\\x5d\\x7f-\\u00ff]+|\\x22(?:[^\\x0d\\x22\\x5c\\u0080-\\u00ff]|\\x5c[\\x00-\\x7f])*\\x22))*\\x40(?:(?:(?:[a-zA-Z\\d](?:[-a-zA-Z\\d]*[a-zA-Z\\d])?)\\.)*(?:[a-zA-Z](?:[-a-zA-Z\\d]*[a-zA-Z\\d])?)\\.?)?[^\\x00-\\x20\\x22\\x28\\x29\\x2c\\x2e\\x3a-\\x3c\\x3e\\x40\\x5b-\\x5d\\x7f-\\u00ff]+\\z","http://www.w3.org/2001/XMLSchema#string","","http://purl.org/linked-delta/supplant"]
            ["_:g647540","http://www.w3.org/ns/shacl#path","http://schema.org/email","http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode","","http://purl.org/linked-delta/supplant"]
            ["_:g647600","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","https://ns.ontola.io/form#Group","http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode","","http://purl.org/linked-delta/supplant"]
            ["_:g647600","https://ns.ontola.io/form#collapsible","false","http://www.w3.org/2001/XMLSchema#boolean","","http://purl.org/linked-delta/supplant"]
            ["_:g647600","https://ns.ontola.io/form#fields","_:g670600","http://www.w3.org/1999/02/22-rdf-syntax-ns#blankNode","","http://purl.org/linked-delta/supplant"]
            ["_:g670600","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq","http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode","","http://purl.org/linked-delta/supplant"]
            ["_:g670600","http://www.w3.org/1999/02/22-rdf-syntax-ns#_0","_:g647540","http://www.w3.org/1999/02/22-rdf-syntax-ns#blankNode","","http://purl.org/linked-delta/supplant"]
            ["_:g647660","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","https://ns.ontola.io/form#TextInput","http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode","","http://purl.org/linked-delta/supplant"]
            ["_:g647660","http://schema.org/name","Redirect url","http://www.w3.org/2001/XMLSchema#string","","http://purl.org/linked-delta/supplant"]
            ["_:g647660","https://ns.ontola.io/core#helperText","","http://www.w3.org/2001/XMLSchema#string","","http://purl.org/linked-delta/supplant"]
            ["_:g647660","https://ns.ontola.io/form#placeholder","","http://www.w3.org/2001/XMLSchema#string","","http://purl.org/linked-delta/supplant"]
            ["_:g647660","http://www.w3.org/ns/shacl#datatype","http://www.w3.org/2001/XMLSchema#string","http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode","","http://purl.org/linked-delta/supplant"]
            ["_:g647660","http://www.w3.org/ns/shacl#maxCount","1","http://www.w3.org/2001/XMLSchema#integer","","http://purl.org/linked-delta/supplant"]
            ["_:g647660","http://www.w3.org/ns/shacl#path","https://ns.ontola.io/core#redirectUrl","http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode","","http://purl.org/linked-delta/supplant"]
            ["_:g647700","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","https://ns.ontola.io/form#HiddenGroup","http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode","","http://purl.org/linked-delta/supplant"]
            ["_:g647700","https://ns.ontola.io/form#collapsible","false","http://www.w3.org/2001/XMLSchema#boolean","","http://purl.org/linked-delta/supplant"]
            ["_:g647700","https://ns.ontola.io/form#hidden","true","http://www.w3.org/2001/XMLSchema#boolean","","http://purl.org/linked-delta/supplant"]
            ["_:g647700","https://ns.ontola.io/form#fields","_:g670620","http://www.w3.org/1999/02/22-rdf-syntax-ns#blankNode","","http://purl.org/linked-delta/supplant"]
            ["_:g670620","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq","http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode","","http://purl.org/linked-delta/supplant"]
            ["_:g670620","http://www.w3.org/1999/02/22-rdf-syntax-ns#_0","_:g647660","http://www.w3.org/1999/02/22-rdf-syntax-ns#blankNode","","http://purl.org/linked-delta/supplant"]
            ["_:g647800","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","https://ns.ontola.io/form#FooterGroup","http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode","","http://purl.org/linked-delta/supplant"]
            ["_:g647800","https://ns.ontola.io/form#collapsible","false","http://www.w3.org/2001/XMLSchema#boolean","","http://purl.org/linked-delta/supplant"]
            ["_:g647500","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","https://ns.ontola.io/form#Page","http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode","","http://purl.org/linked-delta/supplant"]
            ["_:g647500","https://ns.ontola.io/form#groups","_:g670640","http://www.w3.org/1999/02/22-rdf-syntax-ns#blankNode","","http://purl.org/linked-delta/supplant"]
            ["_:g670640","http://www.w3.org/1999/02/22-rdf-syntax-ns#type","http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq","http://www.w3.org/1999/02/22-rdf-syntax-ns#namedNode","","http://purl.org/linked-delta/supplant"]
            ["_:g670640","http://www.w3.org/1999/02/22-rdf-syntax-ns#_0","_:g647600","http://www.w3.org/1999/02/22-rdf-syntax-ns#blankNode","","http://purl.org/linked-delta/supplant"]
            ["_:g670640","http://www.w3.org/1999/02/22-rdf-syntax-ns#_1","_:g647700","http://www.w3.org/1999/02/22-rdf-syntax-ns#blankNode","","http://purl.org/linked-delta/supplant"]
            ["_:g670640","http://www.w3.org/1999/02/22-rdf-syntax-ns#_2","_:g647800","http://www.w3.org/1999/02/22-rdf-syntax-ns#blankNode","","http://purl.org/linked-delta/supplant"]
            ["_:g647500","https://ns.ontola.io/form#footerGroup","_:g647800","http://www.w3.org/1999/02/22-rdf-syntax-ns#blankNode","","http://purl.org/linked-delta/supplant"
        """.trimIndent()

        val scoped = scopeBlankNodes(source)

        val blankNodeCounts = listOf(
            "g647800" to 4,
            "g647500" to 4,
            "g670640" to 5,
            "g670620" to 3,
            "g647700" to 5,
            "g647660" to 8,
            "g670600" to 3,
            "g647600" to 4,
            "g647540" to 10,
            "g670580" to 3,
        )

        blankNodeCounts.forEach { (id, count) ->
            assertEquals(count, id.toRegex().findAll(scoped!!).count(), message = "Wrong count for $id")
        }
    }
}
