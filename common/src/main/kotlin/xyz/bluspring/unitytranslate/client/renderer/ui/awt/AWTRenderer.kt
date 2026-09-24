package xyz.bluspring.unitytranslate.client.renderer.ui.awt

import com.google.common.collect.HashMultimap
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import org.joml.Matrix3x2f
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIGraphics
import xyz.bluspring.unitytranslate.client.ClientPlatformProxy
import xyz.bluspring.unitytranslate.client.renderer.GpuFormat
import xyz.bluspring.unitytranslate.client.renderer.GpuTexture
import xyz.bluspring.unitytranslate.client.renderer.ui.texture.DirectTextureReference
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.util.*
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

class AWTRenderer {
    private val layers = Stack<Layer>()

    private val renderedLayers = mutableListOf<Layer>()

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

                layer.cleanup()

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
//            graphics.outline(layer.x.toFloat(), layer.y.toFloat(), (layer.x + layer.width).toFloat(), (layer.y + layer.height).toFloat(), 1f, -1)
            graphics.popMatrix()
        }
    }

    companion object {
        @JvmRecord
        private data class Size2i(val width: Int, val height: Int)

        private val guiScale: Float
            get() = ClientPlatformProxy.instance.guiScale.toFloat()

        private val imageLayers = HashMultimap.create<Size2i, LayerReference>()
        private val allocatedLayers = HashMultimap.create<Size2i, Int>()
        private val clearingLayers = HashMultimap.create<Size2i, LayerReference>()

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
                    reference.texture.textureView.close()
                    reference.texture.texture.close()
                    reference.nativeImage.close()
                    reference.image.flush()

                    this.imageLayers.remove(size, reference)
                }
            }
        }
    }
}
