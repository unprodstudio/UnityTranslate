package xyz.bluspring.unitytranslate.client.renderer.ui

import com.google.common.collect.HashMultimap
import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import org.joml.Matrix3x2f
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIGraphics
import xyz.bluspring.unitytranslate.client.renderer.ui.texture.DirectTextureReference
import java.awt.AlphaComposite
import java.awt.image.BufferedImage
import java.util.*

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

        val reference: LayerReference by lazy {
            tryAllocateLayer(width, height)
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
            val encoder = RenderSystem.getDevice().createCommandEncoder()
            encoder.writeToTexture(layer.reference.texture.texture, layer.reference.copyToImage())

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

        @JvmRecord
        data class LayerReference(val texture: DirectTextureReference, val image: BufferedImage, val nativeImage: NativeImage) {
            fun copyToImage(): NativeImage {
                for (x in 0 until this.image.width) {
                    for (y in 0 until this.image.height) {
                        this.nativeImage.setPixel(x, y, this.image.getRGB(x, y))
                    }
                }

                return this.nativeImage
            }
        }

        private val imageLayers = HashMultimap.create<Size2i, LayerReference>()
        private val allocatedLayers = HashMultimap.create<Size2i, Int>()
        private val clearingLayers = HashMultimap.create<Size2i, LayerReference>()

        fun tryAllocateLayer(width: Int, height: Int): LayerReference {
            val size = Size2i(width, height)
            val references = this.imageLayers.get(size)
            val allocations = this.allocatedLayers.get(size)

            for ((index, reference) in references.withIndex()) {
                if (!allocations.contains(index)) {
                    this.allocatedLayers.put(size, index)
                    return reference
                }
            }

            // Create a new layer otherwise
            val texture = RenderSystem.getDevice().createTexture({ "UnityTranslate Layer ${references.size + 1} for size ${width}x${height}" },
                GpuTexture.USAGE_TEXTURE_BINDING or GpuTexture.USAGE_COPY_DST, GpuFormat.RGBA8_UNORM, width, height, 1, 1)

            val textureReference = DirectTextureReference(texture)
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val nativeImage = NativeImage(NativeImage.Format.RGBA, width, height, false)
            val reference = LayerReference(textureReference, image, nativeImage)

            this.allocatedLayers.put(size, references.size)
            this.imageLayers.put(size, reference)

            return reference
        }

        fun dereferenceLayer(width: Int, height: Int, reference: LayerReference) {
            val size = Size2i(width, height)
            this.clearingLayers.put(size, reference)
        }

        private fun resetLayer(size: Size2i, reference: LayerReference) {
            val references = this.imageLayers.get(size)

            val index = references.indexOf(reference)
            this.allocatedLayers.remove(size, index)

            // clear the image
            val graphics = reference.image.createGraphics()
            graphics.composite = AlphaComposite.Clear
            graphics.fillRect(0, 0, size.width, size.height)
            graphics.composite = AlphaComposite.SrcOver
            graphics.dispose()
        }

        fun resetAllLayers() {
            for ((size, reference) in this.clearingLayers.entries()) {
                this.resetLayer(size, reference)
            }

            this.clearingLayers.clear()
        }
    }
}
