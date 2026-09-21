package xyz.bluspring.unitytranslate.client.renderer.ui.awt.draw

import org.joml.Matrix3x2fc
import xyz.bluspring.unitytranslate.api.v2.util.ColorMatrix
import xyz.bluspring.unitytranslate.client.renderer.ui.awt.shape.RRShape
import java.awt.BasicStroke
import java.awt.Graphics2D

@JvmRecord
data class RoundedRectangleOutlineDrawCall(
    override val matrix: Matrix3x2fc,
    val radius: Float, val thickness: Float,
    override val x1: Float, override val y1: Float,
    override val x2: Float, override val y2: Float,
    override val colorMatrix: ColorMatrix,
) : ShapedDrawCall {
    override fun draw(graphics: Graphics2D, guiScale: Float) {
        super.draw(graphics, guiScale)
        graphics.stroke = BasicStroke(this.thickness)
        graphics.draw(RRShape(width, height, this.radius))
    }
}
