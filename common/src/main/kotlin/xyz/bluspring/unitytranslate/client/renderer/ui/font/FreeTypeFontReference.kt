package xyz.bluspring.unitytranslate.client.renderer.ui.font

import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import xyz.bluspring.unitytranslate.api.v2.client.gui.font.FontReference
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.util.MaxRectsBinPack
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.InputStream
import kotlin.math.roundToInt

class FreeTypeFontReference(stream: InputStream, val fontSize: Float) : FontReference {
    val font: Font = Font.createFonts(stream)[0]
        .deriveFont(fontSize)
    private val context = FontRenderContext(AffineTransform(), true, false)
    private val glyphs = mutableMapOf<Int, GlyphInfo>()
    private var atlas = NativeImage(NativeImage.Format.LUMINANCE_ALPHA, 256, 256, false)
        set(value) {
            if (value.width < field.width || value.height < field.height)
                throw IllegalArgumentException("Resizing to a smaller size is unsupported! (original: ${field.width}x${field.height}, new: ${value.width}x${value.height})")

            this.rects.reset(value.width, value.height)

            // Reset all currently stored glyphs, make sure we're actually adding to a new atlas
            val currentGlyphs = glyphs.toMap()
            glyphs.clear()
            for ((codepoint, current) in currentGlyphs) {
                val newRect = this.rects.insert(current.width, current.height)
                field.copyRect(value, current.u, current.v, newRect.x, newRect.y, current.width, current.height, false, false)

                glyphs[codepoint] = GlyphInfo(newRect.x, newRect.y, newRect.width, newRect.height)
            }

            field.close()
            field = value
        }

    private val rects = MaxRectsBinPack(atlas.width, atlas.height)

    override val lineHeight: Int
        get() = Toolkit.getDefaultToolkit().getFontMetrics(this.font).height

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
            for (part in component.split(" ")) {
                val combined = TextComponent.literal(part).withStyle(style)
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
            for (part in component.split(" ")) {
                val combined = TextComponent.literal(part).withStyle(style)
                val width = this.width(combined)

                if (currentWidth + width < maxWidth) {
                    main.append(combined)
                    currentWidth += width
                }
            }
        })

        return main
    }

    private fun getOrCreateGlyphInfo(codepoint: Int): GlyphInfo {
        return this.glyphs.computeIfAbsent(codepoint) { _ ->
            val vector = this.font.createGlyphVector(this.context, "${Char(codepoint)}")
            val bounds = vector.getPixelBounds(this.context, 0f, 0f)
            val width = bounds.width.coerceAtLeast(1)
            val height = bounds.height.coerceAtLeast(1)

            val rasterized = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val graphics = rasterized.createGraphics()
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            graphics.font = this.font
            graphics.color = Color.WHITE
            graphics.drawGlyphVector(vector, -bounds.x.toFloat(), -bounds.y.toFloat())
            graphics.dispose()

            var rect = this.rects.insert(width, height)
            if (rect.width == 0 || rect.height == 0) {
                // We need to bump up the atlas size, we ran out of space.
                this.increaseAtlasSize()

                rect = this.rects.insert(width, height)
                if (rect.width == 0 || rect.height == 0) {
                    throw IllegalArgumentException("Could not fit glyph of size ${width}x${height} into atlas! (current atlas size: ${this.rects.width}x${this.rects.height})")
                }
            }

            // Copy the glyph onto the atlas now.
            for (x in 0 until width) {
                for (y in 0 until height) {
                    this.atlas.setPixel(x, y, rasterized.getRGB(x, y))
                }
            }

            GlyphInfo(rect.x, rect.y, rect.width, rect.height)
        }
    }

    private fun increaseAtlasSize() {
        val currentSize = this.atlas.width
        val maxTextureSize = this.maxTextureSize()
        if (currentSize >= maxTextureSize) {
            throw IllegalStateException("Cannot increase atlas size any further! The atlas is already at $currentSize, and the current renderer only supports up to $maxTextureSize!")
        }

        val nextSize = nextPowerOfTwo(currentSize)
        this.atlas = NativeImage(NativeImage.Format.LUMINANCE_ALPHA, nextSize, nextSize, false)
    }

    private fun nextPowerOfTwo(n: Int): Int {
        if (n <= 1) return 1
        val highestBit = Integer.highestOneBit(n - 1)
        return highestBit shl 1
    }

    private fun maxTextureSize(): Int {
        return RenderSystem.getDevice().deviceInfo.limits.maxTextureSize
    }

    @JvmRecord
    private data class GlyphInfo(
        val u: Int, val v: Int,
        val width: Int, val height: Int,
    )
}
