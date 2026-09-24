package xyz.bluspring.unitytranslate.client.renderer.ui.texture

import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.MapCodec
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import xyz.bluspring.unitytranslate.UnityTranslate
import xyz.bluspring.unitytranslate.client.renderer.GpuFormat
import xyz.bluspring.unitytranslate.client.renderer.GpuTexture
import xyz.bluspring.unitytranslate.client.renderer.GpuTextureView
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.inputStream
import kotlin.io.path.name

data class FileTextureReference(val texturePath: Path) : CloseableTextureReference("file") {
    private val nativeImage = try {
        NativeImage.read(texturePath.inputStream(StandardOpenOption.READ))
    } catch (e: Throwable) {
        UnityTranslate.logger.error("Failed to load texture $texturePath!", e)
        MissingTextureAtlasSprite.generateMissingImage()
    }

    private lateinit var internalTexture: GpuTexture
    private lateinit var internalTextureView: GpuTextureView

    override val textureView: GpuTextureView
        get() {
            if (!::internalTexture.isInitialized) {
                this.internalTexture = RenderSystem.getDevice().createTexture({ "UnityTranslate File Texture Reference to ${this.texturePath.name}" }, 0,
                    when (this.nativeImage.format()) {
                        NativeImage.Format.RGBA -> GpuFormat.RGBA8_UNORM
                        NativeImage.Format.RGB -> GpuFormat.RGB8_UNORM
                        NativeImage.Format.LUMINANCE_ALPHA -> GpuFormat.D32_FLOAT_S8_UINT
                        NativeImage.Format.LUMINANCE -> GpuFormat.D32_FLOAT
                    }, this.nativeImage.width, this.nativeImage.height, 0, 0)

                this.internalTextureView = RenderSystem.getDevice().createTextureView(this.internalTexture)
            }

            return this.internalTextureView
        }

    override fun close() {
        if (this.nativeImage.isClosed)
            return

        this.nativeImage.close()

        if (this::internalTexture.isInitialized) {
            this.internalTextureView.close()
            this.internalTexture.close()
        }
    }

    override val imageWidth: Int
        get() = this.nativeImage.width

    override val imageHeight: Int
        get() = this.nativeImage.height

    companion object {
        @JvmField val CODEC: MapCodec<FileTextureReference> = Codec.STRING
            .comapFlatMap({
                try {
                    val path = Path(it)
                    DataResult.success(path)
                } catch (e: InvalidPathException) {
                    DataResult.error { "Failed to parse path $it: ${e.message}" }
                }
            }, {
                it.absolutePathString()
            })
            .xmap(::FileTextureReference, FileTextureReference::texturePath)
            .fieldOf("path")
    }
}
