package xyz.bluspring.unitytranslate.client.renderer.ui.texture

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.resources.Identifier
import xyz.bluspring.unitytranslate.client.renderer.GpuTextureView

data class MinecraftSpriteTextureReference(val atlasId: Identifier, val spriteId: Identifier) : AbstractTextureReference("minecraft_sprite") {
    val atlas: TextureAtlas
        get() = Minecraft.getInstance().atlasManager.getAtlasOrThrow(this.atlasId)

    val sprite: TextureAtlasSprite
        get() = this.atlas.getSprite(this.spriteId)

    override val textureView: GpuTextureView
        get() = this.atlas.textureView

    override val u0: Float = this.sprite.u0
    override val u1: Float = this.sprite.u1
    override val v0: Float = this.sprite.v0
    override val v1: Float = this.sprite.v1

    override val width: Int
        get() = ((this.sprite.u1 - this.sprite.u0) * this.imageWidth).toInt()

    override val height: Int
        get() = ((this.sprite.v1 - this.sprite.v0) * this.imageHeight).toInt()

    override val imageWidth: Int
        get() = this.atlas.texture.getWidth(0)

    override val imageHeight: Int
        get() = this.atlas.texture.getHeight(0)

    companion object {
        @JvmField val CODEC: MapCodec<MinecraftSpriteTextureReference> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Identifier.CODEC.fieldOf("atlas")
                    .forGetter(MinecraftSpriteTextureReference::atlasId),
                Identifier.CODEC.fieldOf("sprite")
                    .forGetter(MinecraftSpriteTextureReference::spriteId)
            )
                .apply(instance, ::MinecraftSpriteTextureReference)
        }
    }
}
