package xyz.bluspring.unitytranslate.client.renderer.ui.awt

import org.joml.Matrix3x2f
import xyz.bluspring.unitytranslate.client.renderer.ui.awt.draw.AWTDrawCall
import java.awt.Graphics2D

data class Layer(
    val x: Int, val y: Int,
    val width: Int, val height: Int,
    val matrix: Matrix3x2f,
) {
    val hasImage: Boolean
        get() = this.drawCalls.isNotEmpty()

    internal val drawCalls = mutableListOf<AWTDrawCall>()

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

    fun addDrawCall(call: AWTDrawCall) {
        this.drawCalls.add(call)
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
