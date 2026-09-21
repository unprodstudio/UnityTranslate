package xyz.bluspring.unitytranslate.client.renderer.ui.awt

import org.joml.Matrix3x2f
import org.joml.Matrix3x2fc
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.client.renderer.ui.awt.draw.DrawCall
import xyz.bluspring.unitytranslate.client.renderer.ui.awt.draw.TextDrawCall
import xyz.bluspring.unitytranslate.client.renderer.ui.font.FreeTypeFontReference
import java.awt.Graphics2D

data class Layer(
    val x: Int, val y: Int,
    val width: Int, val height: Int,
    val matrix: Matrix3x2f,
) {
    val hasImage: Boolean
        get() = this.drawCalls.isNotEmpty()

    internal val drawCalls = mutableListOf<DrawCall>()

    val reference: LayerReference by lazy {
        AWTRenderer.tryAllocateLayer(width, height)
    }

    val hash: Int
        get() {
            var hash = 0

            for (call in this.drawCalls) {
                hash = 31 * hash + call.hashCode()
            }

            return hash
        }

    fun addCall(font: FreeTypeFontReference, matrix: Matrix3x2fc, text: TextComponent, color: Int, x: Float, y: Float, dropShadow: Boolean) {
        this.drawCalls.add(TextDrawCall(font, matrix, text, color, x, y, dropShadow))
    }

    private var graphics: Graphics2D? = null

    fun requestGraphics(): Graphics2D {
        if (this.graphics != null) {
            return this.graphics!!
        }

        val graphics = this.reference.image.createGraphics()
        this.graphics = graphics
        return graphics
    }

    fun cleanup() {
        if (this.graphics != null) {
            this.graphics!!.dispose()
            this.graphics = null
        }
    }
}
