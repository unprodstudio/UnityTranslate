package xyz.bluspring.unitytranslate.client.renderer.ui.texture

import com.mojang.serialization.MapCodec
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.AbstractTexture
import net.minecraft.resources.Identifier
import xyz.bluspring.unitytranslate.client.renderer.GpuTextureView

data class MinecraftTextureReference(val id: Identifier) : AbstractTextureReference("minecraft_texture") {
    val texture: AbstractTexture
        get() = Minecraft.getInstance().textureManager.getTexture(this.id)

    override val textureView: GpuTextureView
        get() = texture.textureView

    override val imageWidth: Int
        get() = this.texture.texture.getWidth(0)

    override val imageHeight: Int
        get() = this.texture.texture.getHeight(0)

    companion object {
        @JvmField val CODEC: MapCodec<MinecraftTextureReference> = Identifier.CODEC.xmap(::MinecraftTextureReference, MinecraftTextureReference::id)
            .fieldOf("texture_id")
    }
}
