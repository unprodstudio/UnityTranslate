package xyz.bluspring.unitytranslate.client.renderer.ui.awt.texture

import xyz.bluspring.unitytranslate.api.v2.util.ColorMatrix
import xyz.bluspring.unitytranslate.client.renderer.ui.awt.draw.AWTDrawCall.Companion.asAWT
import java.awt.Color
import java.awt.GradientPaint
import java.awt.RadialGradientPaint
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.util.*

data class GradientTexture(override val width: Int, override val height: Int, val colorMatrix: ColorMatrix) : AWTTexture {
    override val image: BufferedImage by lazy {
        val image = BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        val fullAlpha = Color(0, 0, 0, 0)
        val trbl = GradientPaint(this.width.toFloat(), 0f, this.colorMatrix.topRight.asAWT, 0f, this.height.toFloat(), this.colorMatrix.bottomLeft.asAWT)

        val radius = this.width.toFloat() - (this.width.toFloat() / 4f)
        val tl = RadialGradientPaint(Point2D.Float(0f, 0f), radius, floatArrayOf(0f, 1f), arrayOf(this.colorMatrix.topLeft.asAWT, fullAlpha))
        val br = RadialGradientPaint(Point2D.Float(this.width.toFloat(), this.height.toFloat()), radius, floatArrayOf(0f, 1f), arrayOf(this.colorMatrix.bottomRight.asAWT, fullAlpha))

        graphics.paint = trbl
        graphics.fillRect(0, 0, this.width, this.height)

        graphics.paint = tl
        graphics.fillRect(0, 0, this.width, this.height)

        graphics.paint = br
        graphics.fillRect(0, 0, this.width, this.height)

        graphics.dispose()

        image
    }

    companion object {
        private val textures = Collections.newSetFromMap<GradientTexture>(WeakHashMap())

        fun request(width: Int, height: Int, matrix: ColorMatrix): GradientTexture {
            for (texture in textures) {
                if (texture.width == width && texture.height == height && texture.colorMatrix == matrix)
                    return texture
            }

            val texture = GradientTexture(width, height, matrix)
            this.textures.add(texture)
            return texture
        }
    }
}
