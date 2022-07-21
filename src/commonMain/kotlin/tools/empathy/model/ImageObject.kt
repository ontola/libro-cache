package tools.empathy.model

import tools.empathy.serialization.DataSlice
import tools.empathy.serialization.Entity
import tools.empathy.serialization.Value
import tools.empathy.serialization.dt
import tools.empathy.serialization.field
import tools.empathy.serialization.id
import tools.empathy.serialization.int
import tools.empathy.serialization.record
import tools.empathy.serialization.s
import tools.empathy.serialization.type
import tools.empathy.vocabularies.Ontola
import tools.empathy.vocabularies.Schema

data class ImageObject(
    override val id: Value.Id,
    val filename: String? = null,
    val contentUrl: Value.Id? = null,
    val datePublished: Value.DateTime? = null,
    val description: String? = null,
    val encodingFormat: String? = null,
    val isPartOf: Value.Id? = null,
    val potentialAction: Value.Id? = null,
    val thumbnail: Value.Id? = null,
    val uploadDate: Value.DateTime? = null,
    val contentSource: Value.Id? = null,
    val copyUrl: Value.Id? = null,
    val fileUsage: String? = null,
    val actionsMenu: Value.Id? = null,
    val destroyAction: Value.Id? = null,
    val imagePositionY: Int? = null,
    val imgUrl64x64: Value.Id? = null,
    val imgUrl256x256: Value.Id? = null,
    val imgUrl568x400: Value.Id? = null,
    val imgUrl1500x2000: Value.Id? = null,
    val organization: Value.Id? = null,
) : Entity

fun DataSlice.add(it: ImageObject): Value.Id = record(it.id) {
    type(Schema.ImageObject)

    field("http://dbpedia.org/ontology/filename") { s(it.filename) }
    field(Schema.contentUrl) { id(it.contentUrl) }
    field(Schema.description) { id(it.description) }
    field(Schema.datePublished) { dt(it.datePublished) }
    field(Schema.encodingFormat) { s(it.encodingFormat) }
    field(Schema.isPartOf) { id(it.isPartOf) }
    field(Schema.potentialAction) { id(it.potentialAction) }
    field(Schema.thumbnail) { id(it.thumbnail) }
    field(Schema.uploadDate) { dt(it.uploadDate) }
    field("https://argu.co/ns/core#contentSource") { id(it.contentSource) }
    field("https://argu.co/ns/core#copyUrl") { id(it.copyUrl) }
    field("https://argu.co/ns/core#fileUsage") { s(it.fileUsage) }
    field(Ontola.actionsMenu) { id(it.actionsMenu) }
    field(Ontola.destroyAction) { id(it.destroyAction) }
    field(Ontola.imagePositionY) { int(it.imagePositionY) }
    field(Ontola.imgUrl64x64) { id(it.imgUrl64x64) }
    field(Ontola.imgUrl256x256) { id(it.imgUrl256x256) }
    field(Ontola.imgUrl568x400) { id(it.imgUrl568x400) }
    field(Ontola.imgUrl1500x2000) { id(it.imgUrl1500x2000) }
    field(Ontola.organization) { id(it.organization) }
}
