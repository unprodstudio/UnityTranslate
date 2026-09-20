package xyz.bluspring.unitytranslate.client.renderer.ui

import com.google.common.collect.HashMultimap
import com.google.common.collect.MapMaker
import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fc
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIGraphics
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.api.v2.util.ARGBHelper
import xyz.bluspring.unitytranslate.client.ClientPlatformProxy
import xyz.bluspring.unitytranslate.client.renderer.ui.font.FreeTypeFontReference
import xyz.bluspring.unitytranslate.client.renderer.ui.font.FreeTypeFontReference.Companion.asAwtStyle
import xyz.bluspring.unitytranslate.client.renderer.ui.texture.DirectTextureReference
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.util.*
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

class AWTRenderer {
    private val layers = Stack<Layer>()

    private val renderedLayers = mutableListOf<Layer>()

    data class Layer(
        val x: Int, val y: Int,
        val width: Int, val height: Int,
        val matrix: Matrix3x2f,
    ) {
        var hasImage = false
            private set

        internal val drawCalls = mutableListOf<TextDrawCall>()

        val reference: LayerReference by lazy {
            tryAllocateLayer(width, height)
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
            this.hasImage = true
            this.drawCalls.add(TextDrawCall(font, matrix, text, color, x, y, dropShadow))
        }

        @JvmRecord
        data class TextDrawCall(val font: FreeTypeFontReference, val matrix: Matrix3x2fc, val text: TextComponent, val color: Int, val x: Float, val y: Float, val dropShadow: Boolean) {
            fun draw(layer: Layer) {
                val graphics = layer.image.createGraphics()
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

                graphics.transform(AffineTransform(matrix.m00(), matrix.m01(), matrix.m10(), matrix.m11(), matrix.m20(), matrix.m21()))

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
                        graphics.color = Color(ARGBHelper.multiply(currentColor, ARGBHelper.colorFromFloat(1f, 0.2f, 0.2f, 0.2f)))
                        graphics.drawString(component, currentX + guiScale, y + guiScale)
                        graphics.color = Color(currentColor)
                    }

                    graphics.drawString(component, currentX, y)
                    currentX += bounds.width.toFloat()
                })

                graphics.dispose()
            }
        }

        val image: BufferedImage by lazy {
            this.hasImage = true
            this.reference.image
        }
    }

    fun pushLayer(x: Int, y: Int, width: Int, height: Int, matrix: Matrix3x2f) {
        this.layers.push(Layer(x, y, width, height, matrix))
    }

    fun peek(): Layer {
        return this.layers.peek()
    }

    fun flushLayer(graphics: UIGraphics) {
        val layer = this.layers.pop()
        if (layer.hasImage) {
            val currentHash = layer.hash
            if (currentHash != layer.reference.lastLayerHash) {
                for (call in layer.drawCalls) {
                    call.draw(layer)
                }

                layer.reference.lastLayerHash = currentHash

                val encoder = RenderSystem.getDevice().createCommandEncoder()
                encoder.writeToTexture(layer.reference.texture.texture, layer.reference.copyToImage())
            }

            graphics.pushMatrix()
            graphics.set(layer.matrix)
            graphics.blit(layer.x.toFloat(), layer.y.toFloat(), layer.x.toFloat() + layer.width.toFloat(), layer.y.toFloat() + layer.height.toFloat(),
                0f, 0f, 1f, 1f, layer.reference.texture)
            graphics.popMatrix()

            this.renderedLayers.add(layer)

            dereferenceLayer(layer.width, layer.height, layer.reference)
        }
    }

    fun flushLast(graphics: UIGraphics) {
        this.flushLayer(graphics)

        for (layer in this.renderedLayers) {
            graphics.pushMatrix()
            graphics.set(layer.matrix)
            graphics.outline(layer.x.toFloat(), layer.y.toFloat(), (layer.x + layer.width).toFloat(), (layer.y + layer.height).toFloat(), 1f, -1)
            graphics.popMatrix()
        }
    }

    companion object {
        @JvmRecord
        private data class Size2i(val width: Int, val height: Int)

        data class LayerReference(val texture: DirectTextureReference, val image: BufferedImage, val nativeImage: NativeImage) {
            var lastAccess = System.currentTimeMillis()
                internal set

            internal var lastLayerHash = 0
            internal val hasher = hashers.computeIfAbsent(image.width * image.height * 4, ::FixedLengthHashCode)

            fun copyToImage(): NativeImage {
                val pixels = (this.image.raster.dataBuffer as DataBufferInt).data
                val buffer = this.nativeImage.pixelBytes
                buffer.asIntBuffer().put(pixels)

                return this.nativeImage
            }
        }

        // By https://richardstartin.github.io/posts/explicit-intent-and-even-faster-hash-codes
        internal class FixedLengthHashCode(maxLength: Int) {
            private val coefficients = IntArray(maxLength + 1)

            init {
                coefficients[maxLength] = 1
                var i = maxLength - 1
                while (i >= 0) {
                    coefficients[i] = 31 * coefficients[i + 1]
                    --i
                }
            }

            fun hashCode(value: IntArray): Int {
                var result = coefficients[0]
                var i = 0
                while (i < value.size && i < coefficients.size - 1) {
                    result += coefficients[i + 1] * value[i]
                    ++i
                }

                return result
            }
        }

        private val guiScale: Float
            get() = ClientPlatformProxy.instance.guiScale.toFloat()

        private val imageLayers = HashMultimap.create<Size2i, LayerReference>()
        private val allocatedLayers = HashMultimap.create<Size2i, Int>()
        private val clearingLayers = HashMultimap.create<Size2i, LayerReference>()

        private val hashers = MapMaker().weakValues().makeMap<Int, FixedLengthHashCode>()

        fun tryAllocateLayer(width: Int, height: Int): LayerReference {
            val size = Size2i(ceil(width * guiScale).toInt(), ceil(height * guiScale).toInt())
            val references = this.imageLayers.get(size)
            val allocations = this.allocatedLayers.get(size)

            for ((index, reference) in references.withIndex()) {
                if (!allocations.contains(index)) {
                    reference.lastAccess = System.currentTimeMillis()
                    this.allocatedLayers.put(size, index)
                    return reference
                }
            }

            // Create a new layer otherwise
            val texture = RenderSystem.getDevice().createTexture({ "UnityTranslate Layer ${references.size + 1} for size ${size.width}x${size.height}" },
                GpuTexture.USAGE_TEXTURE_BINDING or GpuTexture.USAGE_COPY_DST, GpuFormat.RGBA8_UNORM, size.width, size.height, 1, 1)

            val textureReference = DirectTextureReference(texture)
            val image = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB)
            val nativeImage = NativeImage(NativeImage.Format.RGBA, size.width, size.height, false)
            val reference = LayerReference(textureReference, image, nativeImage)

            this.allocatedLayers.put(size, references.size)
            this.imageLayers.put(size, reference)

            return reference
        }

        fun dereferenceLayer(width: Int, height: Int, reference: LayerReference) {
            val size = Size2i(ceil(width * guiScale).toInt(), ceil(height * guiScale).toInt())
            this.clearingLayers.put(size, reference)
        }

        private fun resetLayer(size: Size2i, reference: LayerReference) {
            val references = this.imageLayers.get(size)

            val index = references.indexOf(reference)
            this.allocatedLayers.remove(size, index)

            // clear the image
            val pixels = (reference.image.raster.dataBuffer as DataBufferInt).data
            Arrays.fill(pixels, 0)
        }

        private val MAX_LAST_ACCESS_TIME = 30.seconds.inWholeMilliseconds
        fun resetAllLayers() {
            for ((size, reference) in this.clearingLayers.entries()) {
                this.resetLayer(size, reference)
            }

            this.clearingLayers.clear()

            // Remove old references
            val currentTime = System.currentTimeMillis()
            for ((size, reference) in this.imageLayers.entries().toList()) {
                if (currentTime - reference.lastAccess >= MAX_LAST_ACCESS_TIME) {
                    this.imageLayers.remove(size, reference)
                }
            }
        }
    }
}
