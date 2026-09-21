package xyz.bluspring.unitytranslate.client.renderer.ui.awt

import com.mojang.blaze3d.platform.NativeImage
import xyz.bluspring.unitytranslate.client.renderer.ui.texture.DirectTextureReference
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt

data class LayerReference(val texture: DirectTextureReference, val image: BufferedImage, val nativeImage: NativeImage) {
    var lastAccess = System.currentTimeMillis()
        internal set

    internal var lastLayerHash = 0

    fun copyToImage(): NativeImage {
        val pixels = (this.image.raster.dataBuffer as DataBufferInt).data
        val buffer = this.nativeImage.pixelBytes
        buffer.asIntBuffer().put(pixels)

        return this.nativeImage
    }
}
