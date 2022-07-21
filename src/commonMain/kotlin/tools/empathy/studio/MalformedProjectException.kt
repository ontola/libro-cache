package tools.empathy.studio

/**
 * Indicates a [Project] did not meet the requirements to be converted into a [Distribution].
 */
class MalformedProjectException(message: String? = null) : IllegalArgumentException(message)
