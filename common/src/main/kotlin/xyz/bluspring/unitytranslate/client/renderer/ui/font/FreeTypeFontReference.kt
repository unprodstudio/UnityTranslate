package xyz.bluspring.unitytranslate.client.renderer.ui.font

import org.joml.Matrix3x2f
import org.joml.Matrix3x2fc
import xyz.bluspring.unitytranslate.api.v2.client.gui.font.FontReference
import xyz.bluspring.unitytranslate.api.v2.display.text.Style
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.api.v2.util.ARGBHelper
import xyz.bluspring.unitytranslate.client.renderer.ui.AWTRenderer
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.io.InputStream
import kotlin.math.roundToInt

class FreeTypeFontReference(stream: InputStream, val fontSize: Float) : FontReference {
    val font: Font = Font.createFonts(stream)[0]
        .deriveFont(fontSize)
    private val context = FontRenderContext(AffineTransform(), true, false)

    lateinit var awtRenderer: AWTRenderer

    override val lineHeight: Int
        get() = (Toolkit.getDefaultToolkit().getFontMetrics(this.font).height) / 2 + 2

    override fun width(text: TextComponent): Int {
        var width = 0

        text.visit({ component, style ->
            var fontStyle = Font.PLAIN
            if (style.bold == true)
                fontStyle = fontStyle or Font.BOLD

            if (style.italic == true)
                fontStyle = fontStyle or Font.ITALIC

            val font = this.font.deriveFont(fontStyle, this.fontSize)
            width += font.getStringBounds(component, context).width.roundToInt()
        })

        return width
    }

    override fun width(text: String): Int {
        return font.getStringBounds(text, context).width.roundToInt()
    }

    override fun split(
        text: TextComponent,
        maxWidth: Int
    ): List<TextComponent> {
        var currentWidth = 0
        var lastComponent = TextComponent.empty()
        val currentComponents = mutableListOf<TextComponent>()
        text.visit({ component, style ->
            val split = component.split(" ")
            for ((i, part) in split.withIndex()) {
                val combined = TextComponent.literal(part + if (i != split.lastIndex) " " else "").withStyle(style)
                val width = this.width(combined)

                if (currentWidth + width >= maxWidth) {
                    currentComponents.add(lastComponent)
                    lastComponent = combined
                    currentWidth = width
                } else {
                    lastComponent.append(combined)
                    currentWidth += width
                }
            }
        })

        currentComponents.add(lastComponent)
        return currentComponents
    }

    override fun substr(
        text: TextComponent,
        maxWidth: Int
    ): TextComponent {
        val main = TextComponent.empty()
        var currentWidth = 0

        text.visit({ component, style ->
            val split = component.split(" ")
            for ((i, part) in split.withIndex()) {
                val combined = TextComponent.literal(part + if (i != split.lastIndex) " " else "").withStyle(style)
                val width = this.width(combined)

                if (currentWidth + width < maxWidth) {
                    main.append(combined)
                    currentWidth += width
                }
            }
        })

        return main
    }

    fun draw(matrix: Matrix3x2fc, text: TextComponent, x: Float, y: Float, color: Int, dropShadow: Boolean) {
        val layer = this.awtRenderer.peek()
        val matrix = Matrix3x2f(matrix)
        matrix.mul(layer.matrix.invert(Matrix3x2f()))
        matrix.translate(-layer.x.toFloat(), -layer.y.toFloat())

        val graphics = layer.image.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        graphics.transform(AffineTransform(matrix.m00(), matrix.m01(), matrix.m10(), matrix.m11(), matrix.m20(), matrix.m21()))

        var currentX = x
        text.visit({ component, style ->
            val currentColor = ARGBHelper.multiply((style.color ?: -1), color)
            val font = this.font.deriveFont(style.asAwtStyle)
            graphics.font = font

            val bounds = font.getStringBounds(component, graphics.fontRenderContext)
            graphics.color = Color(currentColor)

            if (style.underlined == true)
                graphics.fillRect(x.toInt(), (y + bounds.height).toInt(), bounds.width.toInt(), 1)

            if (style.strikethrough == true)
                graphics.fillRect(x.toInt(), (y + (bounds.height / 2)).toInt(), bounds.width.toInt(), 1)

            if (dropShadow) {
                graphics.color = Color(ARGBHelper.multiply(currentColor, ARGBHelper.colorFromFloat(1f, 0.2f, 0.2f, 0.2f)))
                graphics.drawString(component, currentX + 1, y + 1)
                graphics.color = Color(currentColor)
            }

            graphics.drawString(component, currentX, y)
            currentX += bounds.width.toFloat()
        })

        graphics.dispose()
    }

    private val Style.asAwtStyle: Int
        get() {
            var current = Font.PLAIN
            if (this.bold == true)
                current += Font.BOLD

            if (this.italic == true)
                current += Font.ITALIC

            return current
        }
}
