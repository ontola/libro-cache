package io.ontola.color

private const val threshold = 0.5
private const val LIMIT: UByte = 255u
private const val aR = 0.299
private const val aG = 0.587
private const val aB = 0.117
private val coefficients = listOf(aR, aG, aB)

/**
 * For more info on this see check these links:
 * http://www.w3.org/TR/2008/REC-WCAG20-20081211/#relativeluminancedef
 * https://en.wikipedia.org/wiki/Rec._709#Luma_coefficients
 */
fun Color.luminance(): Double = coefficients
    .mapIndexed { i, d -> (colorComponents[i] / LIMIT).toDouble() * d }
    .reduce { acc, d -> acc + d }

fun Color.isLight() = luminance() > threshold
