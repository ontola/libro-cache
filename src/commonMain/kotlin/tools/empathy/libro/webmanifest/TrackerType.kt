package tools.empathy.libro.webmanifest

import kotlinx.serialization.Serializable

@Serializable
enum class TrackerType {
    GUA,
    GTM,
    PiwikPro,
    Matomo,
}
