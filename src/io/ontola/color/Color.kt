package io.ontola.color

val hexColorLong = Regex("^#([\\p{XDigit}]{2})([\\p{XDigit}]{2})([\\p{XDigit}]{2})$")
val hexColorShort = Regex("^#(\\p{XDigit})(\\p{XDigit})(\\p{XDigit})$")

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
                val m = hexColorLong.matchEntire(cssString)!!
                    .groupValues
                    .slice(1..3)
                    .map(String::hexToUByte)
                Color(m[0], m[1], m[2])
            }
            hexColorShort.matches(cssString) -> {
                val m = hexColorShort.matchEntire(cssString)!!
                    .groupValues
                    .slice(1..3)
                    .map(String::shortHexToUByte)
                Color(m[0], m[1], m[2])
            }
            else -> TODO("Only hex form css strings are supported")
        }
    }
}
