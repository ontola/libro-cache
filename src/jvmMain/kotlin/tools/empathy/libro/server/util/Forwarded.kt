package tools.empathy.libro.server.util

private const val host = "host="
private const val proto = "proto="

fun String.forwardedProto(): String? {
    val place = this.indexOf(proto)
    if (place == -1) {
        return null
    }

    return this.substring(place + proto.length).split(";").firstOrNull()
}

fun String.forwardedHost(): String? {
    val place = this.indexOf(host)
    if (place == -1) {
        return null
    }

    return this.substring(place + host.length).split(";").firstOrNull()
}
