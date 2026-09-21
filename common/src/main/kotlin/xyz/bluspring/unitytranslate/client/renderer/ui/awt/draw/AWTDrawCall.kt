package xyz.bluspring.unitytranslate.client.renderer.ui.awt.draw

import org.joml.Matrix3x2fc
import xyz.bluspring.unitytranslate.client.ClientPlatformProxy
import xyz.bluspring.unitytranslate.client.renderer.ui.awt.Layer
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform

interface AWTDrawCall {
    val matrix: Matrix3x2fc

    fun draw(layer: Layer) {
        val graphics = layer.requestGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        graphics.transform(
            AffineTransform(
                matrix.m00(), matrix.m01(),
                matrix.m10(), matrix.m11(),
                matrix.m20(), matrix.m21()
            )
        )
        this.draw(graphics, guiScale)
    }

    fun draw(graphics: Graphics2D, guiScale: Float)

    companion object {
        val guiScale: Float
            get() = ClientPlatformProxy.instance.guiScale.toFloat()

        val Int.asAWT: Color
            get() = Color(this, true)
    }
}
