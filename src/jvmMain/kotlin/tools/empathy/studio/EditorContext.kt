package tools.empathy.studio

import kotlinx.serialization.Serializable

@Serializable
data class EditorContext(
    val core: String = "",
    val localOntologies: Map<String, String> = emptyMap(),
    val ontologies: Map<String, String> = emptyMap(),
)
