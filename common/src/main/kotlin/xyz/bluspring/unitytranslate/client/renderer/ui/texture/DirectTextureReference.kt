package xyz.bluspring.unitytranslate.client.renderer.ui.texture

import com.mojang.blaze3d.systems.RenderSystem
import xyz.bluspring.unitytranslate.client.renderer.GpuTexture
import xyz.bluspring.unitytranslate.client.renderer.GpuTextureView

data class DirectTextureReference(
    val texture: GpuTexture,
) : CloseableTextureReference("direct") {
    override val textureView: GpuTextureView = RenderSystem.getDevice().createTextureView(this.texture)

    override val imageWidth: Int
        get() = this.texture.getWidth(0)

    override val imageHeight: Int
        get() = this.texture.getHeight(0)

    override fun close() {
        this.textureView.close()
    }
}
