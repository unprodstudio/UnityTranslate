package xyz.bluspring.unitytranslate.api.v2.util

import kotlin.math.floor

/**
 * ARGB colour math helpers.
 */
object ARGBHelper {
    const val MAX_COMPONENT_SIZE = 0xFF

    @JvmStatic
    inline fun Int.alpha(): Int {
        return (this shr 24) and MAX_COMPONENT_SIZE
    }

    @JvmStatic
    inline fun Int.red(): Int {
        return (this shr 16) and MAX_COMPONENT_SIZE
    }

    @JvmStatic
    inline fun Int.green(): Int {
        return (this shr 8) and MAX_COMPONENT_SIZE
    }

    @JvmStatic
    inline fun Int.blue(): Int {
        return this and MAX_COMPONENT_SIZE
    }

    @JvmStatic
    inline fun opaque(color: Int): Int {
        return color.withAlpha(MAX_COMPONENT_SIZE)
    }

    @JvmStatic
    inline fun Int.withAlpha(alpha: Int): Int {
        return (this and 0x00FFFFFF) or (alpha shl 24)
    }

    @JvmStatic
    inline fun Int.withAlpha(alpha: Float): Int {
        return this.withAlpha((alpha * MAX_COMPONENT_SIZE).toInt())
    }

    @JvmStatic
    inline fun Int.multiplyAlpha(multiplier: Float): Int {
        val alpha = this.alpha() / 255f
        return this.withAlpha(alpha * multiplier)
    }

    @JvmStatic
    inline fun Int.splitToInts(): ColorPair<Int> {
        return ColorPair(this.alpha(), this.red(), this.green(), this.blue())
    }

    @JvmStatic
    inline fun Int.splitToFloats(): ColorPair<Float> {
        return ColorPair(this.alpha() / 255f, this.red() / 255f, this.green() / 255f, this.blue() / 255f)
    }

    @JvmStatic
    inline fun color(a: Int, r: Int, g: Int, b: Int): Int {
        // 0xFF_FF_FF_FF
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    @JvmStatic
    inline val Int.opaque: Int
        get() = this.withAlpha(255)

    @JvmStatic
    fun colorFromFloat(a: Float, r: Float, g: Float, b: Float): Int {
        return color((a * MAX_COMPONENT_SIZE).toInt(), (r * MAX_COMPONENT_SIZE).toInt(), (g * MAX_COMPONENT_SIZE).toInt(), (b * MAX_COMPONENT_SIZE).toInt())
    }

    @JvmStatic
    fun srgbLerp(argb1: Int, argb2: Int, delta: Float): Int {
        val a = lerp(argb1.alpha(), argb2.alpha(), delta)
        val r = lerp(argb1.red(), argb2.red(), delta)
        val g = lerp(argb1.green(), argb2.green(), delta)
        val b = lerp(argb1.blue(), argb2.blue(), delta)
        return color(a, r, g, b)
    }

    @JvmStatic
    fun matrixSrgbLerp(argbTopLeft: Int, argbTopRight: Int, argbBottomLeft: Int, argbBottomRight: Int, deltaX: Float, deltaY: Float): Int {
        val topXLerp = srgbLerp(argbTopLeft, argbTopRight, deltaX)
        val bottomXLerp = srgbLerp(argbBottomLeft, argbBottomRight, deltaX)

        return srgbLerp(topXLerp, bottomXLerp, deltaY)
    }

    @JvmStatic
    inline fun multiply(from: Int, to: Int): Int {
        val (fromA, fromR, fromG, fromB) = from.splitToFloats()
        val (toA, toR, toG, toB) = to.splitToFloats()

        return colorFromFloat(fromA * toA, fromR * toR, fromG * toG, fromB * toB)
    }

    private fun lerp(from: Int, to: Int, delta: Float): Int {
        return from + floor(delta * (to - from).toFloat()).toInt()
    }

    @JvmRecord
    data class ColorPair<N : Number>(val a: N, val r: N, val g: N, val b: N)
}
