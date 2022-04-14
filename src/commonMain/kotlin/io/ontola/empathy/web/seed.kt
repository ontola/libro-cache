package io.ontola.empathy.web

import io.ktor.http.Url
import io.ontola.rdf.hextuples.DataType
import io.ontola.rdf.hextuples.Hextuple
import io.ontola.util.absolutize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull

const val supplantGraph = "http://purl.org/linked-delta/supplant"

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

fun Value.toJsonElementMap(): JsonObject = when (this) {
    is Value.GlobalId -> buildJsonObject {
        put("t", JsonPrimitive("id"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
    }
    is Value.LocalId -> buildJsonObject {
        put("t", JsonPrimitive("lid"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
    }
    is Value.Str -> buildJsonObject {
        put("t", JsonPrimitive("s"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
    }
    is Value.Bool -> buildJsonObject {
        put("t", JsonPrimitive("b"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
    }
    is Value.Int -> buildJsonObject {
        put("t", JsonPrimitive("i"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
    }
    is Value.Long -> buildJsonObject {
        put("t", JsonPrimitive("l"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
    }
    is Value.DateTime -> buildJsonObject {
        put("t", JsonPrimitive("dt"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
    }
    is Value.LangString -> buildJsonObject {
        put("t", JsonPrimitive("ls"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
        put("l", JsonPrimitive(this@toJsonElementMap.lang))
    }
    is Value.Primitive -> buildJsonObject {
        put("t", JsonPrimitive("p"))
        put("v", JsonPrimitive(this@toJsonElementMap.value))
        put("dt", JsonPrimitive(this@toJsonElementMap.dataType))
    }
}

fun JsonObject.toValue(): Value {
    val value = (this["v"] as JsonPrimitive).contentOrNull ?: throw Exception("No value for `v` key in Value")

    return when (val type = (this["t"] as JsonPrimitive).content) {
        "id" -> shortenedGlobalId(value, null)
        "lid" -> Value.LocalId(value)
        "s" -> Value.Str(value)
        "b" -> Value.Bool(value)
        "i" -> Value.Primitive(value, (this["l"] as JsonPrimitive).content)
        "l" -> Value.Primitive(value, (this["l"] as JsonPrimitive).content)
        "dt" -> Value.DateTime(value)
        "ls" -> Value.LangString(value, (this["l"] as JsonPrimitive).content)
        "p" -> Value.Primitive(value, (this["dt"] as JsonPrimitive).content)
        else -> throw Exception("Unknown value type $type")
    }
}

typealias DataSlice = Map<String, Record>

fun DataSlice.toHextuples(): List<Hextuple> = buildList {
    for (record in this@toHextuples.values) {
        for ((predicate, value) in record.entries) {
            value.forEach {
                val (dataType, lang) = when (it) {
                    is Value.GlobalId -> Pair("globalId", "")
                    is Value.LocalId -> Pair("localId", "")
                    is Value.Str -> Pair("http://www.w3.org/2001/XMLSchema#string", "")
                    is Value.Bool -> Pair("http://www.w3.org/2001/XMLSchema#boolean", "")
                    is Value.Int -> Pair("http://www.w3.org/2001/XMLSchema#integer", "")
                    is Value.Long -> Pair("http://www.w3.org/2001/XMLSchema#long", "")
                    is Value.DateTime -> Pair("http://www.w3.org/2001/XMLSchema#dateTime", "")
                    is Value.Primitive -> Pair(it.dataType, "")
                    is Value.LangString -> Pair("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString", it.lang)
                }

                add(
                    Hextuple(
                        record.id.value,
                        predicate,
                        it.value,
                        DataType.fromValue(dataType),
                        lang,
                        supplantGraph,
                    )
                )
            }
        }
    }
}

fun List<Hextuple?>.toSlice(websiteIRI: Url? = null): DataSlice = buildMap {
    for (hex in this@toSlice) {
        if (hex == null || hex.graph == "http://purl.org/link-lib/meta") continue

        if (hex.graph != supplantGraph) throw Error("Non-supplant statement: $hex")

        val record = this.getOrPut(shortenedGlobalIdString(hex.subject, websiteIRI)) {
            val id = if (hex.subject.startsWith("_"))
                Value.LocalId(hex.subject)
            else
                shortenedGlobalId(hex.subject, websiteIRI)
            Record(id)
        }
        val field = record.entries.getOrPut(shortenedGlobalIdString(hex.predicate, websiteIRI)) { arrayOf() }
        val value = hex.toValue(websiteIRI)
        record[shortenedGlobalIdString(hex.predicate, websiteIRI)] = arrayOf(*field, value)

        put(shortenedGlobalIdString(hex.subject, websiteIRI), record)
    }
}

fun Hextuple.toValue(websiteIRI: Url?): Value = when (datatype) {
    is DataType.GlobalId -> shortenedGlobalId(value, websiteIRI)
    is DataType.LocalId -> Value.LocalId(value)
    else -> when (datatype.value()) {
        "http://www.w3.org/2001/XMLSchema#string" -> Value.Str(value)
        "http://www.w3.org/2001/XMLSchema#boolean" -> Value.Bool(value)
        "http://www.w3.org/2001/XMLSchema#integer" -> Value.Int(value)
        "http://www.w3.org/2001/XMLSchema#long" -> Value.Long(value)
        "http://www.w3.org/2001/XMLSchema#dateTime" -> Value.DateTime(value)
        else -> if (language == "")
            Value.Primitive(value, datatype.value())
        else
            Value.LangString(value, language)
    }
}

fun shortenedGlobalIdString(v: String, websiteIRI: Url?): String = shortMap[v] ?: websiteIRI.absolutize(v)

fun shortenedGlobalId(v: String, websiteIRI: Url?): Value.GlobalId = Value.GlobalId(shortenedGlobalIdString(v, websiteIRI))

val shortMap = mapOf(
    "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property" to "Property",
    "http://www.w3.org/2000/01/rdf-schema#isDefinedBy" to "isDefinedBy",
    "http://www.w3.org/2000/01/rdf-schema#Class" to "Class",
    "http://schema.org/name" to "name",
    "https://ns.ontola.io/form#Field" to "Field",
    "http://schema.org/Thing" to "Thing",
    "http://schema.org/CreativeWork" to "CreativeWork",
    "http://www.w3.org/2000/01/rdf-schema#comment" to "rdfscomment",
    "http://schema.org/rangeIncludes" to "rangeIncludes",
    "http://schema.org/domainIncludes" to "domainIncludes",

    "http://www.w3.org/1999/02/22-rdf-syntax-ns#_0" to "0",
    "https://ns.ontola.io/core#_destroy" to "Destroy",
    "https://argu.co/ns/core#acceptTerms" to "acceptTerms",
    "https://argu.co/ns/core#acceptedTerms" to "acceptedTerms",
    "https://ns.ontola.io/core#action" to "action",
    "http://purl.org/link-lib/actionBody" to "actionBody",
    "https://argu.co/ns/core#actionName" to "actionName",
    "http://schema.org/actionStatus" to "actionStatus",
    "https://ns.ontola.io/core#actionsMenu" to "actionsMenu",
    "https://ns.ontola.io/core#actor" to "actor",
    "https://ns.ontola.io/core#actorType" to "actorType",
    "https://argu.co/ns/rivm#additionalIntroductionInformation" to "additionalIntroductionInformation",
    "http://schema.org/addressCountry" to "addressCountry",
    "https://argu.co/ns/core#alias" to "alias",
    "https://ns.ontola.io/core#allowedExternalSources" to "allowedExternalSources",
    "http://www.w3.org/ns/shacl#and" to "and",
    "https://argu.co/ns/core#anonymous" to "anonymous",
    "https://argu.co/ns/core#arguPublication" to "arguPublication",
    "https://argu.co/ns/core#argumentColumns" to "argumentColumns",
    "https://argu.co/ns/rivm#attachmentPublicationDate" to "attachmentPublicationDate",
    "https://argu.co/ns/core#attachments" to "attachments",
    "https://ns.ontola.io/core#audience" to "audience",
    "https://ns.ontola.io/core#banners" to "banners",
    "https://ns.ontola.io/core#bannersManagement" to "bannersManagement",
    "https://ns.ontola.io/core#baseCollection" to "baseCollection",
    "https://argu.co/ns/core#blogPosts" to "blogPosts",
    "https://argu.co/ns/core#budgetMax" to "budgetMax",
    "https://argu.co/ns/core#budgetShops" to "budgetShops",
    "https://argu.co/ns/rivm#businessSection" to "businessSection",
    "https://argu.co/ns/rivm#businessSectionEmployees" to "businessSectionEmployees",
    "http://schema.org/caption" to "caption",
    "https://argu.co/ns/core#cart" to "cart",
    "https://argu.co/ns/core#cartDetail" to "cartDetail",
    "https://argu.co/ns/core#cartDetails" to "cartDetails",
    "https://argu.co/ns/rivm#categories" to "categories",
    "https://argu.co/ns/core#checkoutAction" to "checkoutAction",
    "https://argu.co/ns/core#childrenPlacements" to "childrenPlacements",
    "http://www.w3.org/ns/shacl#class" to "class",
    "http://www.w3.org/2004/02/skos/core#closeMatch" to "closeMatch",
    "http://www.w3.org/ns/shacl#closed" to "closed",
    "https://ns.ontola.io/form#collapsible" to "collapsible",
    "https://ns.ontola.io/core#collectionDisplay" to "collectionDisplay",
    "http://schema.org/color" to "color",
    "http://schema.org/comment" to "comment",
    "https://argu.co/ns/rivm#commentsAllowed" to "commentsAllowed",
    "https://argu.co/ns/core#commentsCount" to "commentsCount",
    "https://argu.co/ns/core#communicateAction" to "communicateAction",
    "https://argu.co/ns/rivm#communication" to "communication",
    "https://argu.co/ns/rivm#competence" to "competence",
    "http://purl.org/linked-data/cube#component" to "component",
    "https://argu.co/ns/core#conArguments" to "conArguments",
    "https://argu.co/ns/core#conArgumentsCount" to "conArgumentsCount",
    "https://argu.co/ns/core#confirmationString" to "confirmationString",
    "https://argu.co/ns/core#confirmed" to "confirmed",
    "https://argu.co/ns/core#confirmedAt" to "confirmedAt",
    "https://argu.co/ns/rivm#conflictAndPrioritization" to "conflictAndPrioritization",
    "https://argu.co/ns/rivm#contactAllowed" to "contactAllowed",
    "https://argu.co/ns/rivm#contactInfo" to "contactInfo",
    "https://argu.co/ns/core#contentSource" to "contentSource",
    "http://schema.org/contentUrl" to "contentUrl",
    "https://argu.co/ns/rivm#continuous" to "continuous",
    "https://argu.co/ns/core#convertToClass" to "convertToClass",
    "https://argu.co/ns/core#convertibleClasses" to "convertibleClasses",
    "https://argu.co/ns/core#copyUrl" to "copyUrl",
    "https://argu.co/ns/rivm#costExplanation" to "costExplanation",
    "https://argu.co/ns/core#coupon" to "coupon",
    "https://argu.co/ns/core#couponBatches" to "couponBatches",
    "https://argu.co/ns/core#couponCount" to "couponCount",
    "https://argu.co/ns/core#coupons" to "coupons",
    "https://ns.ontola.io/core#coverPhoto" to "coverPhoto",
    "https://argu.co/ns/core#createBlogPostPermission" to "createBlogPostPermission",
    "https://argu.co/ns/core#createCommentPermission" to "createCommentPermission",
    "https://argu.co/ns/core#createConArgumentPermission" to "createConArgumentPermission",
    "https://argu.co/ns/core#createForumPermission" to "createForumPermission",
    "https://argu.co/ns/core#createMotionPermission" to "createMotionPermission",
    "https://argu.co/ns/core#createPermission" to "createPermission",
    "https://argu.co/ns/core#createProArgumentPermission" to "createProArgumentPermission",
    "https://argu.co/ns/core#createQuestionPermission" to "createQuestionPermission",
    "https://argu.co/ns/core#createVote" to "createVote",
    "https://argu.co/ns/core#createVotePermission" to "createVotePermission",
    "https://argu.co/ns/core#CreativeWorkType" to "creativeWorkType",
    "https://argu.co/ns/core#creative_works" to "creativeWorks",
    "http://schema.org/creator" to "creator",
    "https://argu.co/ns/core#currentPassword" to "currentPassword",
    "https://argu.co/ns/core#currentPhase" to "currentPhase",
    "https://argu.co/ns/core#currentVote" to "currentVote",
    "https://argu.co/ns/core#customActions" to "customActions",
    "https://argu.co/ns/core#customFormFields" to "customFormFields",
    "http://purl.org/linked-data/cube#dataSet" to "dataSet",
    "http://www.w3.org/ns/shacl#datatype" to "datatype",
    "http://schema.org/dateCreated" to "dateCreated",
    "http://schema.org/dateModified" to "dateModified",
    "http://schema.org/datePublished" to "datePublished",
    "http://schema.org/dateRead" to "dateRead",
    "https://argu.co/ns/core#dbSchema" to "dbSchema",
    "http://www.w3.org/ns/shacl#deactivated" to "deactivated",
    "https://argu.co/ns/core#decisionState" to "decisionState",
    "https://argu.co/ns/core#decisionsEmails" to "decisionsEmails",
    "https://argu.co/ns/core#defaultDisplay" to "defaultDisplay",
    "https://argu.co/ns/core#defaultOptionsVocab" to "defaultOptionsVocab",
    "https://argu.co/ns/core#defaultSorting" to "defaultSorting",
    "https://ns.ontola.io/form#defaultValue" to "defaultValue",
    "http://www.w3.org/ns/shacl#description" to "description",
    "https://argu.co/ns/core#destination" to "destination",
    "https://argu.co/ns/core#destroyPermission" to "destroyPermission",
    "https://argu.co/ns/core#destroyStrategy" to "destroyStrategy",
    "http://purl.org/linked-data/cube#dimension" to "dimension",
    "https://argu.co/ns/core#discoverable" to "discoverable",
    "https://argu.co/ns/core#discussions" to "discussions",
    "http://www.w3.org/ns/shacl#disjoint" to "disjoint",
    "https://ns.ontola.io/core#dismissAction" to "dismissAction",
    "https://ns.ontola.io/core#dismissButton" to "dismissButton",
    "http://schema.org/downloadUrl" to "downloadUrl",
    "https://argu.co/ns/core#draft" to "draft",
    "https://argu.co/ns/core#edge" to "edge",
    "https://argu.co/ns/rivm#effectivityResearchMethod" to "effectivityResearchMethod",
    "http://schema.org/email" to "email",
    "https://argu.co/ns/core#emailAddresses" to "emailAddresses",
    "https://argu.co/ns/core#emails" to "emails",
    "http://schema.org/embedUrl" to "embedUrl",
    "http://schema.org/encodingFormat" to "encodingFormat",
    "http://schema.org/endDate" to "endDate",
    "http://www.w3.org/ns/shacl#equals" to "equals",
    "https://argu.co/ns/rivm#ergonomics" to "ergonomics",
    "http://schema.org/error" to "error",
    "http://www.w3.org/2004/02/skos/core#exactMatch" to "exactMatch",
    "https://argu.co/ns/core#expiresAt" to "expiresAt",
    "https://argu.co/ns/core#explanation" to "explanation",
    "https://argu.co/ns/core#exportStatus" to "exportStatus",
    "https://argu.co/ns/core#externalIRI" to "externalIRI",
    "https://ns.ontola.io/core#fail" to "fail",
    "https://ns.ontola.io/core#favoriteAction" to "favoriteAction",
    "https://ns.ontola.io/form#fields" to "fields",
    "https://argu.co/ns/core#fileUsage" to "fileUsage",
    "http://dbpedia.org/ontology/filename" to "filename",
    "https://ns.ontola.io/core#filterCount" to "filterCount",
    "https://ns.ontola.io/core#filterKey" to "filterKey",
    "https://ns.ontola.io/core#filterOptions" to "filterOptions",
    "https://ns.ontola.io/core#filterOptionsIn" to "filterOptionsIn",
    "https://ns.ontola.io/core#filterValue" to "filterValue",
    "http://www.w3.org/ns/shacl#flags" to "flags",
    "https://ns.ontola.io/core#followMenu" to "followMenu",
    "https://argu.co/ns/core#followsCount" to "followsCount",
    "https://ns.ontola.io/form#footerGroup" to "footerGroup",
    "http://purl.org/link-lib/forceRender" to "forceRender",
    "https://ns.ontola.io/form#form" to "form",
    "https://argu.co/ns/core#formFieldType" to "formFieldType",
    "https://argu.co/ns/core#formType" to "formType",
    "https://ns.ontola.io/core#forms/inputs/select/displayProp" to "forms::Inputs::Select::Displayprop",
    "https://argu.co/ns/core#forums" to "forums",
    "http://schema.org/geo" to "geo",
    "https://argu.co/ns/core#geoCoordinates" to "geoCoordinates",
    "https://ns.ontola.io/core#googleTagManager" to "googleTagManager",
    "https://ns.ontola.io/core#googleUac" to "googleUac",
    "https://argu.co/ns/core#grantSet" to "grantSet",
    "https://argu.co/ns/core#grantSetKey" to "grantSetKey",
    "https://argu.co/ns/core#grantSets" to "grantSets",
    "https://argu.co/ns/core#grantedGroups" to "grantedGroups",
    "https://argu.co/ns/core#grantedSets" to "grantedSets",
    "https://argu.co/ns/core#grants" to "grants",
    "http://www.w3.org/ns/shacl#group" to "group",
    "https://ns.ontola.io/core#groupBy" to "groupBy",
    "https://argu.co/ns/core#groupId" to "groupId",
    "https://ns.ontola.io/form#groupedOptions" to "groupedOptions",
    "https://ns.ontola.io/form#groups" to "groups",
    "https://argu.co/ns/core#hasAnalytics" to "hasAnalytics",
    "http://www.w3.org/ns/org#hasMember" to "hasMember",
    "http://www.w3.org/ns/shacl#hasValue" to "hasValue",
    "https://argu.co/ns/core#headerBackground" to "headerBackground",
    "https://argu.co/ns/core#headerText" to "headerText",
    "https://ns.ontola.io/core#helperText" to "helperText",
    "https://ns.ontola.io/form#hidden" to "hidden",
    "https://ns.ontola.io/core#hideHeader" to "hideHeader",
    "https://ns.ontola.io/core#hideLanguageSwitcher" to "hideLanguageSwitcher",
    "http://schema.org/homeLocation" to "homeLocation",
    "https://ns.ontola.io/core#homeMenuImage" to "homeMenuImage",
    "https://ns.ontola.io/core#homeMenuLabel" to "homeMenuLabel",
    "http://xmlns.com/foaf/0.1/homepage" to "homepage",
    "https://ns.ontola.io/core#href" to "href",
    "http://schema.org/httpMethod" to "httpMethod",
    "http://schema.org/identifier" to "identifier",
    "http://www.w3.org/ns/shacl#ignoredProperties" to "ignoredProperties",
    "http://schema.org/image" to "image",
    "https://ns.ontola.io/core#imagePositionY" to "imagePositionY",
    "https://ns.ontola.io/core#imgUrl64x64" to "imgUrl64x64",
    "https://ns.ontola.io/core#imgUrl256x256" to "imgUrl256x256",
    "https://ns.ontola.io/core#imgUrl568x400" to "imgUrl568x400",
    "https://ns.ontola.io/core#imgUrl1500x2000" to "imgUrl1500x2000",
    "https://argu.co/ns/core#important" to "important",
    "http://www.w3.org/ns/shacl#in" to "in",
    "https://argu.co/ns/core#inReplyTo" to "inReplyTo",
    "https://argu.co/ns/rivm#independent" to "independent",
    "http://schema.org/industry" to "industry",
    "https://argu.co/ns/rivm#interventionEffects" to "interventionEffects",
    "https://argu.co/ns/rivm#interventionGoal" to "interventionGoal",
    "https://argu.co/ns/rivm#interventions" to "interventions",
    "https://argu.co/ns/core#interventionsCount" to "interventionsCount",
    "https://argu.co/ns/core#introFinished" to "introFinished",
    "https://argu.co/ns/core#invertArguments" to "invertArguments",
    "https://argu.co/ns/core#isDraft" to "isDraft",
    "http://schema.org/isPartOf" to "isPartOf",
    "http://schema.org/itemOffered" to "itemOffered",
    "https://www.w3.org/ns/activitystreams#items" to "items",
    "http://www.w3.org/2000/01/rdf-schema#label" to "label",
    "http://schema.org/language" to "language",
    "http://www.w3.org/ns/shacl#languageIn" to "languageIn",
    "https://argu.co/ns/core#lastActivityAt" to "lastActivityAt",
    "http://schema.org/latitude" to "latitude",
    "http://www.w3.org/ns/shacl#lessThan" to "lessThan",
    "http://www.w3.org/ns/shacl#lessThanOrEquals" to "lessThanOrEquals",
    "https://ns.ontola.io/core#liveUpdates" to "liveUpdates",
    "https://argu.co/ns/core#locale" to "locale",
    "http://schema.org/location" to "location",
    "https://argu.co/ns/core#locationQuery" to "locationQuery",
    "http://schema.org/longitude" to "longitude",
    "https://argu.co/ns/rivm#managementInvolvement" to "managementInvolvement",
    "https://argu.co/ns/core#mapQuestion" to "mapQuestion",
    "https://argu.co/ns/core#markAsImportant" to "markAsImportant",
    "https://ns.ontola.io/core#matomoHost" to "matomoHost",
    "https://ns.ontola.io/core#matomoSiteId" to "matomoSiteId",
    "https://ns.ontola.io/core#maxCount" to "maxCount",
    "http://www.w3.org/ns/shacl#maxExclusive" to "maxExclusive",
    "https://ns.ontola.io/core#maxInclusive" to "maxInclusive",
    "https://ns.ontola.io/core#maxInclusiveLabel" to "maxInclusiveLabel",
    "https://ns.ontola.io/core#maxLength" to "maxLength",
    "http://purl.org/linked-data/cube#measure" to "measure",
    "https://argu.co/ns/rivm#measureOwner" to "measureOwner",
    "http://www.w3.org/ns/org#member" to "member",
    "http://www.w3.org/ns/org#memberOf" to "memberOf",
    "https://ns.ontola.io/core#menuItems" to "menuItems",
    "https://argu.co/ns/core#menuLabel" to "menuLabel",
    "https://ns.ontola.io/core#menus" to "menus",
    "http://www.w3.org/ns/shacl#message" to "message",
    "https://ns.ontola.io/core#minCount" to "minCount",
    "http://www.w3.org/ns/shacl#minExclusive" to "minExclusive",
    "https://ns.ontola.io/core#minInclusive" to "minInclusive",
    "https://ns.ontola.io/core#minInclusiveLabel" to "minInclusiveLabel",
    "https://ns.ontola.io/core#minLength" to "minLength",
    "https://argu.co/ns/rivm#moreInfo" to "moreInfo",
    "https://argu.co/ns/core#motions" to "motions",
    "https://argu.co/ns/core#motionsCount" to "motionsCount",
    "https://argu.co/ns/rivm#motivationAndCommitment" to "motivationAndCommitment",
    "https://ns.ontola.io/core#mountAction" to "mountAction",
    "https://argu.co/ns/core#moveTo" to "moveTo",
    "https://argu.co/ns/core#nameSingular" to "nameSingular",
    "https://argu.co/ns/rivm#natureOfCosts" to "natureOfCosts",
    "https://ns.ontola.io/core#navigationsMenu" to "navigationsMenu",
    "https://argu.co/ns/core#newsEmails" to "newsEmails",
    "https://www.w3.org/ns/activitystreams#next" to "next",
    "http://www.w3.org/ns/shacl#node" to "node",
    "http://www.w3.org/ns/shacl#nodeKind" to "nodeKind",
    "http://www.w3.org/ns/shacl#not" to "not",
    "https://www.w3.org/ns/activitystreams#object" to "object",
    "http://purl.org/linked-data/cube#observation" to "observation",
    "https://argu.co/ns/core#offers" to "offers",
    "https://ns.ontola.io/core#oneClick" to "oneClick",
    "https://argu.co/ns/rivm#oneOffCosts" to "oneOffCosts",
    "https://argu.co/ns/rivm#oneOffCostsScore" to "oneOffCostsScore",
    "http://schema.org/option" to "option",
    "https://argu.co/ns/core#optionsVocab" to "optionsVocab",
    "http://www.w3.org/ns/shacl#or" to "or",
    "http://purl.org/linked-data/cube#order" to "order",
    "https://argu.co/ns/core#orderDetails" to "orderDetails",
    "http://schema.org/orderedItem" to "orderedItem",
    "https://argu.co/ns/core#orders" to "orders",
    "https://ns.ontola.io/core#organization" to "organization",
    "https://argu.co/ns/rivm#organizationName" to "organizationName",
    "https://argu.co/ns/core#otp" to "otp",
    "https://ns.ontola.io/core#otpActive" to "otpActive",
    "https://ns.ontola.io/core#parentMenu" to "parentMenu",
    "https://www.w3.org/ns/activitystreams#partOf" to "partOf",
    "https://ns.ontola.io/core#pass" to "pass",
    "https://ns.ontola.io/core#password" to "password",
    "https://ns.ontola.io/core#passwordConfirmation" to "passwordConfirmation",
    "http://www.w3.org/ns/shacl#path" to "path",
    "http://www.w3.org/ns/shacl#pattern" to "pattern",
    "https://argu.co/ns/core#pdfPage" to "pdfPage",
    "https://argu.co/ns/core#pdfPositionX" to "pdfPositionX",
    "https://argu.co/ns/core#pdfPositionY" to "pdfPositionY",
    "https://argu.co/ns/rivm#peopleAndResources" to "peopleAndResources",
    "https://argu.co/ns/core#permission" to "permission",
    "https://argu.co/ns/core#permissionGroups" to "permissionGroups",
    "https://argu.co/ns/core#permittedAction" to "permittedAction",
    "https://argu.co/ns/rivm#phases" to "phases",
    "https://argu.co/ns/core#photoAttribution" to "photoAttribution",
    "https://argu.co/ns/core#pinned" to "pinned",
    "https://argu.co/ns/core#pinnedAt" to "pinnedAt",
    "https://ns.ontola.io/core#piwikProHost" to "piwikProHost",
    "https://ns.ontola.io/core#piwikProSiteId" to "piwikProSiteId",
    "https://ns.ontola.io/form#placeholder" to "placeholder",
    "https://argu.co/ns/core#placementType" to "placementType",
    "https://argu.co/ns/rivm#plansAndProcedure" to "plansAndProcedure",
    "https://ns.ontola.io/core#pluralLabel" to "pluralLabel",
    "http://schema.org/postalCode" to "postalCode",
    "https://argu.co/ns/core#predicate" to "predicate",
    "https://www.w3.org/ns/activitystreams#prev" to "prev",
    "https://argu.co/ns/core#price" to "price",
    "http://schema.org/priceCurrency" to "priceCurrency",
    "https://argu.co/ns/core#primaryColor" to "primaryColor",
    "https://argu.co/ns/core#primaryEmail" to "primaryEmail",
    "https://argu.co/ns/core#primaryVote" to "primaryVote",
    "https://argu.co/ns/core#proArguments" to "proArguments",
    "https://argu.co/ns/core#proArgumentsCount" to "proArgumentsCount",
    "https://argu.co/ns/core#profile" to "profile",
    "https://argu.co/ns/core#projects" to "projects",
    "http://www.w3.org/ns/shacl#property" to "property",
    "https://argu.co/ns/core#public" to "public",
    "https://www.w3.org/ns/activitystreams#published" to "published",
    "http://www.w3.org/ns/shacl#qualifiedMaxCount" to "qualifiedMaxCount",
    "http://www.w3.org/ns/shacl#qualifiedMinCount" to "qualifiedMinCount",
    "http://www.w3.org/ns/shacl#qualifiedValueShape" to "qualifiedValueShape",
    "https://argu.co/ns/core#questions" to "questions",
    "https://argu.co/ns/core#rawDescription" to "rawDescription",
    "https://argu.co/ns/core#rawHref" to "rawHref",
    "https://argu.co/ns/core#rawImage" to "rawImage",
    "https://argu.co/ns/core#rawResource" to "rawResource",
    "https://argu.co/ns/core#rawSubmitLabel" to "rawSubmitLabel",
    "https://argu.co/ns/core#reactionsEmails" to "reactionsEmails",
    "https://argu.co/ns/rivm#recurringCosts" to "recurringCosts",
    "https://argu.co/ns/rivm#recurringCostsScore" to "recurringCostsScore",
    "https://ns.ontola.io/core#redirectUrl" to "redirectUrl",
    "https://argu.co/ns/core#remoteContentUrl" to "remoteContentUrl",
    "https://argu.co/ns/core#require2fa" to "require2fa",
    "https://argu.co/ns/core#requireCoupon" to "requireCoupon",
    "https://argu.co/ns/core#requireLocation" to "requireLocation",
    "https://ns.ontola.io/core#requiresIntro" to "requiresIntro",
    "https://ns.ontola.io/core#resetPasswordToken" to "resetPasswordToken",
    "https://argu.co/ns/core#resource" to "resource",
    "https://argu.co/ns/core#resourceType" to "resourceType",
    "http://schema.org/result" to "result",
    "https://argu.co/ns/core#reward" to "reward",
    "https://argu.co/ns/rivm#riskReduction" to "riskReduction",
    "http://schema.org/roleName" to "roleName",
    "https://argu.co/ns/core#rootId" to "rootId",
    "http://www.w3.org/2002/07/owl#sameAs" to "sameAs",
    "https://argu.co/ns/rivm#secondOpinion" to "secondOpinion",
    "https://argu.co/ns/rivm#secondOpinionBy" to "secondOpinionBy",
    "https://argu.co/ns/core#secondaryColor" to "secondaryColor",
    "https://argu.co/ns/rivm#securityImproved" to "securityImproved",
    "https://argu.co/ns/rivm#securityImprovedScore" to "securityImprovedScore",
    "https://argu.co/ns/rivm#securityImprovementReason" to "securityImprovementReason",
    "https://argu.co/ns/core#sendMail" to "sendMail",
    "https://argu.co/ns/core#sendNotifications" to "sendNotifications",
    "https://argu.co/ns/core#sessionID" to "sessionID",
    "https://ns.ontola.io/core#sessionMenu" to "sessionMenu",
    "https://ns.ontola.io/core#settingsMenu" to "settingsMenu",
    "http://www.w3.org/ns/shacl#severity" to "severity",
    "https://ns.ontola.io/core#shIn" to "shIn",
    "https://ns.ontola.io/core#shareMenu" to "shareMenu",
    "https://argu.co/ns/core#shortname" to "shortname",
    "https://argu.co/ns/core#shortnameable" to "shortnameable",
    "https://argu.co/ns/core#showHeader" to "showHeader",
    "https://argu.co/ns/core#showPermission" to "showPermission",
    "https://ns.ontola.io/core#sortDirection" to "sortDirection",
    "https://ns.ontola.io/core#sortKey" to "sortKey",
    "http://www.w3.org/ns/shacl#sparql" to "sparql",
    "http://schema.org/startDate" to "startDate",
    "http://purl.org/linked-data/cube#structure" to "structure",
    "https://ns.ontola.io/core#styledHeaders" to "styledHeaders",
    "http://www.w3.org/2000/01/rdf-schema#subClassOf" to "subClassOf",
    "https://argu.co/ns/core#submissionData" to "submissionData",
    "https://argu.co/ns/core#submissionStatus" to "submissionStatus",
    "https://argu.co/ns/core#surveys" to "surveys",
    "https://ns.ontola.io/core#svg" to "svg",
    "https://argu.co/ns/core#system" to "system",
    "https://ns.ontola.io/core#tabsMenu" to "tabsMenu",
    "https://argu.co/ns/core#taggedLabel" to "taggedLabel",
    "https://argu.co/ns/core#taggings" to "taggings",
    "https://www.w3.org/ns/activitystreams#target" to "target",
    "https://argu.co/ns/rivm#targetAudience" to "targetAudience",
    "http://www.w3.org/ns/shacl#targetClass" to "targetClass",
    "http://www.w3.org/ns/shacl#targetNode" to "targetNode",
    "http://www.w3.org/ns/shacl#targetObjectsOf" to "targetObjectsOf",
    "http://www.w3.org/ns/shacl#targetSubjectsOf" to "targetSubjectsOf",
    "https://ns.ontola.io/core#template" to "template",
    "https://ns.ontola.io/core#templateOpts" to "templateOpts",
    "https://argu.co/ns/core#termType" to "termType",
    "https://argu.co/ns/core#terms" to "terms",
    "http://schema.org/text" to "text",
    "http://schema.org/thumbnail" to "thumbnail",
    "https://argu.co/ns/core#time" to "time",
    "http://www.w3.org/2006/time#timeZone" to "timeZone",
    "https://argu.co/ns/rivm#tools" to "tools",
    "https://argu.co/ns/core#topComment" to "topComment",
    "https://argu.co/ns/core#topics" to "topics",
    "https://ns.ontola.io/core#topology" to "topology",
    "https://www.w3.org/ns/activitystreams#totalItems" to "totalItems",
    "http://schema.org/totalPaymentDue" to "totalPaymentDue",
    "https://argu.co/ns/rivm#trainingRequired" to "trainingRequired",
    "https://argu.co/ns/core#transferTo" to "transferTo",
    "https://argu.co/ns/core#trashActivity" to "trashActivity",
    "https://argu.co/ns/core#trashPermission" to "trashPermission",
    "https://argu.co/ns/core#trashed" to "trashed",
    "https://argu.co/ns/core#trashedAt" to "trashedAt",
    "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" to "type",
    "http://www.w3.org/ns/shacl#uniqueLang" to "uniqueLang",
    "https://argu.co/ns/core#unread" to "unread",
    "https://argu.co/ns/core#unreadCount" to "unreadCount",
    "https://argu.co/ns/core#unscoped" to "unscoped",
    "https://argu.co/ns/core#untrashActivity" to "untrashActivity",
    "https://argu.co/ns/core#updatePermission" to "updatePermission",
    "https://www.w3.org/ns/activitystreams#updated" to "updated",
    "http://schema.org/uploadDate" to "uploadDate",
    "http://schema.org/url" to "url",
    "https://argu.co/ns/core#usedCoupons" to "usedCoupons",
    "https://argu.co/ns/core#user" to "user",
    "https://ns.ontola.io/core#userMenu" to "userMenu",
    "https://argu.co/ns/core#view" to "view",
    "https://ns.ontola.io/core#visible" to "visible",
    "https://argu.co/ns/core#voteEvents" to "voteEvents",
    "https://argu.co/ns/core#voteOptions" to "voteOptions",
    "https://argu.co/ns/core#voteableVoteEvent" to "voteableVoteEvent",
    "https://argu.co/ns/core#votes" to "votes",
    "https://argu.co/ns/core#votesConCount" to "votesConCount",
    "https://argu.co/ns/core#votesNeutralCount" to "votesNeutralCount",
    "https://argu.co/ns/core#votesProCount" to "votesProCount",
    "https://argu.co/ns/core#votesPublic" to "votesPublic",
    "https://ns.ontola.io/core#widgetResource" to "widgetResource",
    "https://ns.ontola.io/core#widgetSize" to "widgetSize",
    "https://ns.ontola.io/core#widgets" to "widgets",
    "http://www.w3.org/ns/shacl#xone" to "xone",
    "https://ns.ontola.io/core#zoomLevel" to "zoomLevel",
)
