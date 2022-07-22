package tools.empathy.libro.server.configuration

data class MapsConfig(
    val username: String,
    val key: String,
    val scopes: List<String> = listOf(
        "styles:tiles",
        "styles:read",
        "fonts:read",
        "datasets:read",
    ),
) {
    private val mapboxTileAPIBase = "https://api.mapbox.com/styles/v1"
    private val mapboxTileStyle = "mapbox/streets-v11"

    val mapboxTileURL = "$mapboxTileAPIBase/$mapboxTileStyle"
    val tokenEndpoint = "https://api.mapbox.com/tokens/v2/$username?access_token=$key"
}
