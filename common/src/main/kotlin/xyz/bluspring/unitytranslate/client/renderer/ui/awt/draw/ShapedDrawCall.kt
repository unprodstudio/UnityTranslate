package xyz.bluspring.unitytranslate.client.renderer.ui.awt.draw

import xyz.bluspring.unitytranslate.api.v2.util.ColorMatrix
import xyz.bluspring.unitytranslate.client.renderer.ui.awt.draw.AWTDrawCall.Companion.asAWT
import xyz.bluspring.unitytranslate.client.renderer.ui.awt.texture.GradientTexture
import java.awt.Graphics2D
import java.awt.TexturePaint
import java.awt.geom.Rectangle2D
import kotlin.math.ceil

interface ShapedDrawCall : AWTDrawCall {
    val x1: Float
    val y1: Float
    val x2: Float
    val y2: Float
    val colorMatrix: ColorMatrix

    val width: Float
        get() = this.x2 - this.x1

    val height: Float
        get() = this.y2 - this.y1

    override fun draw(graphics: Graphics2D, guiScale: Float) {
        graphics.translate(this.x1.toDouble(), this.y1.toDouble())

        if (this.colorMatrix.isSolid) {
            graphics.color = this.colorMatrix.topLeft.asAWT
        } else {
            graphics.paint = TexturePaint(GradientTexture.request(ceil(width).toInt(), ceil(height).toInt(), colorMatrix).image, Rectangle2D.Float(x1, y1, width, height))
        }
    }
}
