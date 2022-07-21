package tools.empathy.libro.server.bulk

import java.util.Locale

data class Stats(
    val cached: Int,
    val public: Int,
    val authorized: Int,
) {
    private val total: Int = cached + public + authorized

    private val authorizedRatio = ratio(total, authorized)

    private val cachedRatio = ratio(total, cached)

    private val publicRatio = ratio(total, public)

    override fun toString(): String {
        return arrayOf(
            "items=$total",
            "public=$publicRatio",
            "cached=$cachedRatio",
            "authorized=$authorizedRatio",
        ).joinToString("; ")
    }

    private fun ratio(tot: Int, part: Int): String {
        if (tot == 0) {
            return "1.00"
        } else if (part == 0) {
            return "0.00"
        }

        return "%.2f".format(Locale.ENGLISH, part.toDouble() / tot)
    }
}
