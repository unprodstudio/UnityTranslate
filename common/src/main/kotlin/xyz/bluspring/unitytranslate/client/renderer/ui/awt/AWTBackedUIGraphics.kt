package xyz.bluspring.unitytranslate.client.renderer.ui.awt

import org.joml.Matrix3x2f
import org.joml.Matrix3x2fc
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIGraphics
import xyz.bluspring.unitytranslate.api.v2.util.ColorMatrix
import xyz.bluspring.unitytranslate.client.renderer.ui.awt.draw.RoundedRectangleDrawCall
import xyz.bluspring.unitytranslate.client.renderer.ui.awt.draw.RoundedRectangleOutlineDrawCall

abstract class AWTBackedUIGraphics : UIGraphics {
    val awtRenderer = AWTRenderer()

    abstract fun peekMatrix(): Matrix3x2fc

    override fun roundedFill(x1: Float, y1: Float, x2: Float, y2: Float, radius: Float, matrix: ColorMatrix) {
        this.awtRenderer.peek().addDrawCall(RoundedRectangleDrawCall(Matrix3x2f(this.peekMatrix()), radius, x1, y1, x2, y2, matrix))
    }

    override fun roundedOutline(x1: Float, y1: Float, x2: Float, y2: Float, radius: Float, thickness: Float, matrix: ColorMatrix) {
        this.awtRenderer.peek().addDrawCall(RoundedRectangleOutlineDrawCall(Matrix3x2f(this.peekMatrix()), radius, thickness, x1, y1, x2, y2, matrix))
    }
}
