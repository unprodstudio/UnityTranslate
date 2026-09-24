package xyz.bluspring.unitytranslate.client.renderer.ui.texture

import com.mojang.serialization.Codec
import net.minecraft.resources.Identifier
import xyz.bluspring.unitytranslate.PlatformProxy
import xyz.bluspring.unitytranslate.api.v2.client.gui.TextureReference
import xyz.bluspring.unitytranslate.client.renderer.GpuTextureView
import java.nio.file.Path

abstract class AbstractTextureReference(val type: String) : TextureReference {
    abstract val textureView: GpuTextureView

    override val width: Int
        get() = this.imageWidth

    override val height: Int
        get() = this.imageHeight

    override val u0: Float = 0f
    override val v0: Float = 0f
    override val u1: Float = 1f
    override val v1: Float = 1f

    companion object {
        @JvmField val CODEC: Codec<AbstractTextureReference> = Codec.STRING.dispatch("type", AbstractTextureReference::type) { type ->
            when (type) {
                "direct" -> throw IllegalArgumentException("Direct texture reference is not allowed to be serialized!")
                "file" -> FileTextureReference.CODEC
                "minecraft_sprite" -> {
                    if (PlatformProxy.instance.isStandalone())
                        throw IllegalArgumentException("Minecraft sprites are not allowed to be used in standalone mode!")

                    MinecraftSpriteTextureReference.CODEC
                }
                "minecraft_texture" -> {
                    if (PlatformProxy.instance.isStandalone())
                        throw IllegalArgumentException("Minecraft textures are not allowed to be used in standalone mode!")

                    MinecraftTextureReference.CODEC
                }
                else -> throw IllegalArgumentException("Unknown texture reference type ${type}!")
            }
        }

        @JvmStatic fun file(path: Path): AbstractTextureReference = FileTextureReference(path)
        @JvmStatic fun minecraftTexture(id: String): AbstractTextureReference = MinecraftTextureReference(Identifier.parse(id))
        @JvmStatic fun minecraftSprite(atlas: String, id: String): AbstractTextureReference = MinecraftSpriteTextureReference(Identifier.parse(atlas), Identifier.parse(id))
    }
}
