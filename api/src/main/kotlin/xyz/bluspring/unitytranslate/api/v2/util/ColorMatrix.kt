package xyz.bluspring.unitytranslate.api.v2.util

@JvmRecord
data class ColorMatrix(
    val topLeft: Int, val topRight: Int,
    val bottomLeft: Int, val bottomRight: Int
) {
    val isSolid: Boolean
        get() {
            return this.topLeft == this.topRight && this.topLeft == this.bottomLeft && this.topLeft == this.bottomRight
        }
}
