package tools.empathy.color

val hexColorLong = Regex("^#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})$")
val hexColorShort = Regex("^#([0-9a-fA-F])([0-9a-fA-F])([0-9a-fA-F])$")

fun String.hexToUByte() = toInt(16).toUByte()
fun String.shortHexToUByte() = repeat(2).hexToUByte()

data class Color(
    val red: UByte,
    val green: UByte,
    val blue: UByte,
    val alpha: Double = 1.0,
) {
    val colorComponents: Array<UByte> = arrayOf(red, blue, green)

    companion object {
        fun fromCss(cssString: String): Color = when {
            hexColorLong.matches(cssString) -> {
                val (r, g, b) = hexColorLong.matchEntire(cssString)!!
                    .groupValues
                    .slice(1..3)
                    .map(String::hexToUByte)
                Color(r, g, b)
            }
            hexColorShort.matches(cssString) -> {
                val (r, g, b) = hexColorShort.matchEntire(cssString)!!
                    .groupValues
                    .slice(1..3)
                    .map(String::shortHexToUByte)
                Color(r, g, b)
            }
            else -> TODO("Only hex form css strings are supported")
        }
    }
}
