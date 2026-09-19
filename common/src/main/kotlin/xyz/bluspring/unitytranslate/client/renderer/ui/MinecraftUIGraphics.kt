package xyz.bluspring.unitytranslate.client.renderer.ui

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.FilterMode
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.GuiRenderState
import net.minecraft.util.Mth
import org.joml.Matrix3x2f
import xyz.bluspring.unitytranslate.api.v2.client.gui.TextureReference
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIGraphics
import xyz.bluspring.unitytranslate.api.v2.client.gui.font.FontReference
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.client.ClientPlatformProxy
import xyz.bluspring.unitytranslate.client.renderer.ui.font.FreeTypeFontReference
import xyz.bluspring.unitytranslate.client.renderer.ui.font.MinecraftFontReference
import xyz.bluspring.unitytranslate.client.renderer.ui.minecraft.ColoredBlitRenderState
import xyz.bluspring.unitytranslate.client.renderer.ui.minecraft.ColoredMeshBlitRenderState
import xyz.bluspring.unitytranslate.client.renderer.ui.minecraft.GradientedFillRenderState
import xyz.bluspring.unitytranslate.client.renderer.ui.minecraft.GradientedMeshFillRenderState
import xyz.bluspring.unitytranslate.client.renderer.ui.texture.AbstractTextureReference
import xyz.bluspring.unitytranslate.mixin.accessor.GuiGraphicsExtractorAccessor
import xyz.bluspring.unitytranslate.util.PlatformConversion.asMinecraft
import kotlin.math.roundToInt

class MinecraftUIGraphics(private val graphics: GuiGraphicsExtractor) : UIGraphics {
    private val GuiGraphicsExtractor.guiRenderState: GuiRenderState
        get() = (this as GuiGraphicsExtractorAccessor).`unitytranslate$getGuiRenderState`()

    private val GuiGraphicsExtractor.scissor: ScreenRectangle?
        get() = (this as GuiGraphicsExtractorAccessor).`unityTranslate$getScissorStack`().peek()

    val awtRenderer = AWTRenderer()

    private val visibleArea: ScreenRectangle
        get() {
            if (this.graphics.scissor != null)
                return this.graphics.scissor!!

            return ScreenRectangle(0, 0, this.width, this.height)
        }

    init {
        awtRenderer.pushLayer(0, 0, this.width, this.height)
    }

    private val guiScale: Double
        get() = ClientPlatformProxy.instance.guiScale

    override val width: Int
        get() = graphics.guiWidth()
    override val height: Int
        get() = graphics.guiHeight()

    override fun enableScissor(x: Int, y: Int, width: Int, height: Int) {
        awtRenderer.flushLayer(this)
        graphics.enableScissor(x, y, x + width, y + height)
        awtRenderer.pushLayer((x * guiScale).roundToInt(), (y * guiScale).roundToInt(), (width * guiScale).roundToInt(), (height * guiScale.roundToInt()))
    }

    override fun disableScissor() {
        awtRenderer.flushLayer(this)
        graphics.disableScissor()

        val area = this.visibleArea
        awtRenderer.pushLayer((area.left() * guiScale).roundToInt(), (area.top() * guiScale).roundToInt(), (area.width * guiScale).roundToInt(), (area.height * guiScale.roundToInt()))
    }

    override fun text(font: FontReference, text: TextComponent, x: Float, y: Float, color: Int, dropShadow: Boolean) {
        if (font is MinecraftFontReference) {
            graphics.text(font.font, text.asMinecraft(), x.toInt(), y.toInt(), color, dropShadow)
        } else if (font is FreeTypeFontReference) {
            font.awtRenderer = this.awtRenderer
            font.draw(graphics.pose(), text, x, y, color, dropShadow)
        }
    }

    override fun fill(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        colorTopLeft: Int,
        colorTopRight: Int,
        colorBottomLeft: Int,
        colorBottomRight: Int
    ) {
        graphics.guiRenderState.addGuiElement(GradientedFillRenderState(RenderPipelines.GUI, TextureSetup.noTexture(),
            Matrix3x2f(graphics.pose()),
            graphics.scissor,
            x1, y1, x2, y2,
            colorTopLeft, colorTopRight, colorBottomLeft, colorBottomRight
        ))
    }

    override fun blitWithColor(
        x1: Float, y1: Float, x2: Float, y2: Float,
        u0: Float, v0: Float, u1: Float, v1: Float,
        texture: TextureReference,
        colorTopLeft: Int, colorTopRight: Int, colorBottomLeft: Int, colorBottomRight: Int
    ) {
        if (texture !is AbstractTextureReference)
            throw IllegalStateException("You are not supposed to extend TextureReference! Currently using ${texture::class.java.name}")

        graphics.guiRenderState.addGuiElement(ColoredBlitRenderState(RenderPipelines.GUI_TEXTURED, TextureSetup.singleTexture(texture.textureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)),
            Matrix3x2f(graphics.pose()),
            graphics.scissor,
            x1, y1, x2, y2,

            colorTopLeft, colorTopRight,
            colorBottomLeft, colorBottomRight,

            ((u0 * texture.width) + (texture.u0 * texture.imageWidth)) / texture.imageWidth, ((v0 * texture.height) + (texture.v0 * texture.imageHeight)) / texture.imageHeight,
            ((u1 * texture.width) + (texture.u0 * texture.imageWidth)) / texture.imageWidth, ((v1 * texture.height) + (texture.v0 * texture.imageHeight)) / texture.imageHeight
        ))
    }

    override fun meshBlitWithColor(
        x1: Float, y1: Float, x2: Float, y2: Float,
        x3: Float, y3: Float, x4: Float, y4: Float,
        u1: Float, v1: Float, u2: Float, v2: Float,
        u3: Float, v3: Float, u4: Float, v4: Float,

        texture: TextureReference,
        color1: Int, color2: Int,
        color3: Int, color4: Int
    ) {
        if (texture !is AbstractTextureReference)
            throw IllegalStateException("You are not supposed to extend TextureReference! Currently using ${texture::class.java.name}")

        graphics.guiRenderState.addGuiElement(ColoredMeshBlitRenderState(RenderPipelines.GUI_TEXTURED, TextureSetup.singleTexture(texture.textureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST)),
            Matrix3x2f(graphics.pose()),
            graphics.scissor,
            x1, y1, x2, y2,
            x3, y3, x4, y4,

            color1, color2,
            color3, color4,

            u1, v1, u2, v2,
            u3, v3, u4, v4
        ))
    }

    override fun meshFill(
        x1: Float, y1: Float, x2: Float, y2: Float,
        x3: Float, y3: Float, x4: Float, y4: Float,
        color1: Int, color2: Int,
        color3: Int, color4: Int
    ) {
        graphics.guiRenderState.addGuiElement(GradientedMeshFillRenderState(RenderPipelines.GUI, TextureSetup.noTexture(),
            Matrix3x2f(graphics.pose()),
            graphics.scissor,
            x1, y1, x2, y2,
            x3, y3, x4, y4,
            color1, color2,
            color3, color4
        ))
    }

    override fun pushMatrix() {
        this.graphics.pose().pushMatrix()
    }

    override fun translate(x: Float, y: Float) {
        this.graphics.pose().translate(x, y)
    }

    override fun rotate(degrees: Float) {
        this.graphics.pose().rotate(degrees * Mth.DEG_TO_RAD)
    }

    override fun scale(x: Float, y: Float) {
        this.graphics.pose().scale(x, y)
    }

    override fun popMatrix() {
        this.graphics.pose().popMatrix()
    }

    fun flushLastLayer() {
        this.awtRenderer.flushLayer(this)
    }
}
