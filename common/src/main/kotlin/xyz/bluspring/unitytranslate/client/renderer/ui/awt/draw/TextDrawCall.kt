package xyz.bluspring.unitytranslate.client.renderer.ui.awt.draw

import org.joml.Matrix3x2fc
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.api.v2.util.ARGBHelper
import xyz.bluspring.unitytranslate.client.renderer.ui.font.FreeTypeFontReference
import xyz.bluspring.unitytranslate.client.renderer.ui.font.FreeTypeFontReference.Companion.asAwtStyle
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints

@JvmRecord
data class TextDrawCall(val font: FreeTypeFontReference, override val matrix: Matrix3x2fc, val text: TextComponent, val color: Int, val x: Float, val y: Float, val dropShadow: Boolean) : AWTDrawCall {
    override fun draw(graphics: Graphics2D,  guiScale: Float) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        var currentX = x
        text.visit({ component, style ->
            val currentColor = ARGBHelper.multiply((style.color ?: -1), color)
            val font = font.font.deriveFont(style.asAwtStyle, font.font.size2D * guiScale)
            graphics.font = font

            val bounds = font.getStringBounds(component, graphics.fontRenderContext)
            graphics.color = Color(currentColor)

            if (style.underlined == true)
                graphics.fillRect(x.toInt(), (y + bounds.height).toInt(), bounds.width.toInt(), 1)

            if (style.strikethrough == true)
                graphics.fillRect(x.toInt(), (y + (bounds.height / 2)).toInt(), bounds.width.toInt(), 1)

            if (dropShadow) {
                graphics.color =
                    Color(ARGBHelper.multiply(currentColor, ARGBHelper.colorFromFloat(1f, 0.2f, 0.2f, 0.2f)))
                graphics.drawString(component, currentX + guiScale, y + guiScale)
                graphics.color = Color(currentColor)
            }

            graphics.drawString(component, currentX, y)
            currentX += bounds.width.toFloat()
        })

        graphics.dispose()
    }
}
