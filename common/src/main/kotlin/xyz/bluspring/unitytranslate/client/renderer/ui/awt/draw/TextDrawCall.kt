package xyz.bluspring.unitytranslate.client.renderer.ui.awt.draw

import org.joml.Matrix3x2fc
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.api.v2.util.ARGBHelper
import xyz.bluspring.unitytranslate.client.renderer.ui.font.SmoothFontReference
import xyz.bluspring.unitytranslate.client.renderer.ui.font.SmoothFontReference.Companion.asAwtStyle
import xyz.bluspring.unitytranslate.client.renderer.ui.font.SmoothFontReference.Companion.orElseFallback
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.TextAttribute
import java.awt.font.TextLayout
import java.text.AttributedString

@JvmRecord
data class TextDrawCall(val font: SmoothFontReference, override val matrix: Matrix3x2fc, val text: TextComponent, val color: Int, val x: Float, val y: Float, val dropShadow: Boolean) : AWTDrawCall {
    override fun draw(graphics: Graphics2D,  guiScale: Float) {
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val fullString = text.string
        val colorMap = mutableMapOf<Int, Int>()
        colorMap[0] = color

        var currentColor = Color(color, true)
        var currentIndex = 0

        val attributedText = AttributedString(fullString, mapOf(
            TextAttribute.FOREGROUND to currentColor,
            TextAttribute.FONT to font.font.deriveFont(Font.PLAIN, font.font.size2D * guiScale),
        ))

        text.visit({ component, style ->
            val endIndex = currentIndex + component.length
            if (endIndex == currentIndex) // we didn't end up moving much
                return@visit

            val nextColor = ARGBHelper.multiply((style.color ?: -1), color)
            val font = font.font.deriveFont(style.asAwtStyle, font.font.size2D * guiScale)
            attributedText.addAttribute(TextAttribute.FONT, font, currentIndex, endIndex)

            if (currentColor.rgb != nextColor) {
                val color = Color(nextColor, true)
                attributedText.addAttribute(TextAttribute.FOREGROUND, color, currentIndex, endIndex)
                colorMap[currentIndex] = nextColor
                currentColor = color
            } else {
                attributedText.addAttribute(TextAttribute.FOREGROUND, currentColor, currentIndex, endIndex)
            }

            if (style.underlined == true)
                attributedText.addAttribute(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON, currentIndex, endIndex)

            if (style.strikethrough == true)
                attributedText.addAttribute(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON, currentIndex, endIndex)

            // time to figure out font fallbacks :D

            var fallbackStart = -1
            var currentFont = font
            for ((index, c) in component.withIndex()) {
                if (Character.isWhitespace(c))
                    continue

                val nextFont = font.orElseFallback(c)
                if (nextFont !== currentFont) {
                    if (fallbackStart != -1) {
                        attributedText.addAttribute(TextAttribute.FONT, currentFont.deriveFont(font.style, font.size2D), currentIndex + fallbackStart, currentIndex + index)
                        currentFont = nextFont

                        fallbackStart = if (nextFont !== font)
                            index
                        else
                            -1
                    } else {
                        fallbackStart = index
                        currentFont = nextFont
                    }
                }
            }

            if (fallbackStart != -1) {
                attributedText.addAttribute(TextAttribute.FONT, currentFont.deriveFont(font.style, font.size2D), currentIndex + fallbackStart, endIndex)
            }

            currentIndex += component.length
        })

        if (dropShadow) {
            // unfortunately, AWT doesn't allow us to really do shadow colours, so we have to manually override it.
            val shadowCopy = AttributedString(attributedText.iterator)
            val indices = colorMap.keys.sorted()
            for ((startIndex, color) in colorMap) {
                val shadowColor = Color(ARGBHelper.multiply(color, ARGBHelper.colorFromFloat(1f, 0.2f, 0.2f, 0.2f)))
                val indexOfIndex = indices.indexOf(startIndex)
                if (indexOfIndex < indices.lastIndex) {
                    val nextIndex = indices[indexOfIndex + 1]
                    shadowCopy.addAttribute(TextAttribute.FOREGROUND, shadowColor, startIndex, nextIndex)
                } else {
                    shadowCopy.addAttribute(TextAttribute.FOREGROUND, shadowColor, startIndex, fullString.length)
                }
            }

            val layout = TextLayout(shadowCopy.iterator, graphics.fontRenderContext)
            graphics.color = Color(ARGBHelper.multiply(color, ARGBHelper.colorFromFloat(1f, 0.2f, 0.2f, 0.2f)))
            layout.draw(graphics, x * guiScale + guiScale, y * guiScale + guiScale)
            graphics.color = Color(color)
        }

        val layout = TextLayout(attributedText.iterator, graphics.fontRenderContext)
        layout.draw(graphics, x * guiScale, y * guiScale)
    }
}
