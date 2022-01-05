package io.ontola.transactions

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val operations: Set<Operation>,
)

@Serializable
sealed class Operation {
    abstract val resource: String
    abstract val resourceType: String?
}

/**
 * The [resource] has been created.
 */
@Serializable
class Created(
    override val resource: String,
    override val resourceType: String? = null,
) : Operation()

/**
 * The [resource] has been updated.
 */
@Serializable
class Updated(
    override val resource: String,
    override val resourceType: String? = null,
) : Operation()

/**
 * The [resource] has been converted to another type(s).
 */
@Serializable
class Converted(
    override val resource: String,
    override val resourceType: String? = null,
) : Operation()

/**
 * The [resource] has been re-assigned a new IRI.
 */
@Serializable
class Moved(
    override val resource: String,
    override val resourceType: String? = null,
) : Operation()

/**
 * The [resource] has been published.
 */
@Serializable
class Published(
    override val resource: String,
    override val resourceType: String? = null,
) : Operation()

/**
 * The [resource] has been unpublished.
 */
@Serializable
class Unpublished(
    override val resource: String,
    override val resourceType: String? = null,
) : Operation()

/**
 * The [resource] has permanently been deleted.
 */
@Serializable
class Deleted(
    override val resource: String,
    override val resourceType: String? = null,
) : Operation()
