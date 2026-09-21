package xyz.bluspring.unitytranslate.client.renderer.ui

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.font.TextRenderable
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.util.LightCoordsUtil
import net.minecraft.util.Mth
import org.joml.Matrix3x2f
import org.joml.Matrix3x2fStack
import org.joml.Matrix3x2fc
import org.joml.Matrix4f
import xyz.bluspring.unitytranslate.api.v2.client.gui.TextureReference
import xyz.bluspring.unitytranslate.api.v2.client.gui.font.FontReference
import xyz.bluspring.unitytranslate.api.v2.client.util.ScreenRectangle
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.client.ClientPlatformProxy
import xyz.bluspring.unitytranslate.client.renderer.BatchedGuiRenderer
import xyz.bluspring.unitytranslate.client.renderer.ui.awt.AWTBackedUIGraphics
import xyz.bluspring.unitytranslate.client.renderer.ui.font.FreeTypeFontReference
import xyz.bluspring.unitytranslate.client.renderer.ui.font.MinecraftFontReference
import xyz.bluspring.unitytranslate.client.renderer.ui.texture.AbstractTextureReference
import xyz.bluspring.unitytranslate.util.PlatformConversion.asMinecraft
import java.util.*

class BatchedUIGraphics(private val layer: BatchedGuiRenderer.DrawLayer) : AWTBackedUIGraphics() {
    val matrixStack = Matrix3x2fStack()
    private val scissorState = Stack<ScreenRectangle>()
    private val currentScissor: ScreenRectangle?
        get() = if (this.scissorState.isEmpty()) null else this.scissorState.peek()

    private val visibleArea: ScreenRectangle
        get() {
            if (this.currentScissor != null)
                return this.currentScissor!!

            return ScreenRectangle(0, 0, this.width, this.height)
        }

    override val width: Int = (ClientPlatformProxy.instance.framebuffer.width.toFloat() / ClientPlatformProxy.instance.guiScale.toFloat()).toInt()
    override val height: Int = (ClientPlatformProxy.instance.framebuffer.height.toFloat() / ClientPlatformProxy.instance.guiScale.toFloat()).toInt()

    private val guiScale: Double
        get() = ClientPlatformProxy.instance.guiScale

    private fun Matrix3x2fc.peek(): Matrix4f {
        return Matrix4f(
            this.m00(), this.m01(), 0f, 0f,
            this.m10(), this.m11(), 0f, 0f,
            0f, 0f, 1f, 0f,
            this.m20(), this.m21(), 0f, 1f
        )
    }

    init {
        awtRenderer.pushLayer(0, 0, this.width, this.height, Matrix3x2f())
    }

    override fun enableScissor(x: Int, y: Int, width: Int, height: Int) {
        this.scissorState.push(ScreenRectangle(x, y, width, height))
        awtRenderer.pushLayer(x, y, width, height, Matrix3x2f(this.matrixStack))
    }

    override fun disableScissor() {
        awtRenderer.flushLayer(this)
        this.scissorState.pop()

        val (x, y, width, height) = this.visibleArea
        val matrix = Matrix3x2f()
        if (this.currentScissor != null)
            matrix.set(this.matrixStack)

        if (awtRenderer.peek().matrix != matrix) {
            awtRenderer.flushLayer(this)
            awtRenderer.pushLayer(x, y, width, height, matrix)
        }
    }

    override fun text(font: FontReference, text: TextComponent, x: Float, y: Float, color: Int, dropShadow: Boolean) {
        if (font is MinecraftFontReference) {
            val prepared = font.font.prepareText(text.asMinecraft().visualOrderText, x, y, color, dropShadow, true, 0)
            prepared.visit(object : Font.GlyphVisitor {
                override fun acceptEffect(effect: TextRenderable) {
                    accept(effect)
                }

                override fun acceptGlyph(glyph: TextRenderable.Styled) {
                    accept(glyph)
                }

                private fun accept(glyph: TextRenderable) {
                    val consumer = BatchedGuiRenderer.getBuffer(glyph.guiPipeline(), textures = listOf(
                        BatchedGuiRenderer.Texture("Sampler0", glyph.textureView())
                    ), scissor = currentScissor, layer = this@BatchedUIGraphics.layer)
                    glyph.render(matrixStack.peek(), consumer, LightCoordsUtil.FULL_BRIGHT, false)
                }
            })
        } else if (font is FreeTypeFontReference) {
            font.awtRenderer = this.awtRenderer
            font.draw(this.matrixStack, text, x, y, color, dropShadow, guiScale.toFloat())
        }
    }

    override fun fill(x1: Float, y1: Float, x2: Float, y2: Float, colorTopLeft: Int, colorTopRight: Int, colorBottomLeft: Int, colorBottomRight: Int) {
        val pose = matrixStack.peek()
        val buffer = BatchedGuiRenderer.getBuffer(RenderPipelines.GUI, layer = this.layer)
        buffer.addVertex(pose, x1, y1, 0f).setColor(colorTopLeft)
        buffer.addVertex(pose, x1, y2, 0f).setColor(colorBottomLeft)
        buffer.addVertex(pose, x2, y2, 0f).setColor(colorBottomRight)
        buffer.addVertex(pose, x2, y1, 0f).setColor(colorTopRight)
    }

    override fun blitWithColor(
        x1: Float, y1: Float, x2: Float, y2: Float,
        u0: Float, v0: Float, u1: Float, v1: Float,
        texture: TextureReference,
        colorTopLeft: Int, colorTopRight: Int,
        colorBottomLeft: Int, colorBottomRight: Int
    ) {
        if (texture !is AbstractTextureReference)
            throw IllegalStateException("You are not supposed to extend TextureReference! Currently using ${texture::class.java.name}")

        val pose = matrixStack.peek()
        val buffer = BatchedGuiRenderer.getBuffer(RenderPipelines.GUI_TEXTURED, listOf(BatchedGuiRenderer.Texture("Sampler0", texture.textureView)), scissor = currentScissor, layer = this@BatchedUIGraphics.layer)

        val uStart = ((u0 * texture.width) + (texture.u0 * texture.imageWidth)) / texture.imageWidth
        val vStart = ((v0 * texture.height) + (texture.v0 * texture.imageHeight)) / texture.imageHeight
        val uEnd = ((u1 * texture.width) + (texture.u0 * texture.imageWidth)) / texture.imageWidth
        val vEnd = ((v1 * texture.height) + (texture.v0 * texture.imageHeight)) / texture.imageHeight

        buffer.addVertex(pose, x1, y1, 0f)
            .setUv(uStart, vStart)
            .setColor(colorTopLeft)
        buffer.addVertex(pose, x1, y2, 0f)
            .setUv(uStart, vEnd)
            .setColor(colorBottomLeft)
        buffer.addVertex(pose, x2, y2, 0f)
            .setUv(uEnd, vEnd)
            .setColor(colorBottomRight)
        buffer.addVertex(pose,  x2, y1, 0f)
            .setUv(uEnd, vStart)
            .setColor(colorTopRight)
    }

    override fun meshFill(
        x1: Float, y1: Float, x2: Float, y2: Float,
        x3: Float, y3: Float, x4: Float, y4: Float,
        color1: Int, color2: Int,
        color3: Int, color4: Int
    ) {
        val pose = this.matrixStack.peek()
        val consumer = BatchedGuiRenderer.getBuffer(RenderPipelines.GUI, scissor = currentScissor, layer = this@BatchedUIGraphics.layer)
        consumer.addVertex(pose, x1, y1, 0f) // top left
            .setColor(color1)
        consumer.addVertex(pose, x3, y3, 0f) // bottom left
            .setColor(color3)
        consumer.addVertex(pose, x4, y4, 0f) // bottom right
            .setColor(color4)
        consumer.addVertex(pose, x2, y2, 0f) // top right
            .setColor(color2)
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

        val pose = this.matrixStack.peek()
        val consumer = BatchedGuiRenderer.getBuffer(RenderPipelines.GUI_TEXTURED, listOf(BatchedGuiRenderer.Texture("Sampler0", texture.textureView)), scissor = currentScissor, layer = this@BatchedUIGraphics.layer)
        consumer.addVertex(pose, x1, y1, 0f) // top left
            .setUv(u1, v1)
            .setColor(color1)
        consumer.addVertex(pose, x3, y3, 0f) // bottom left
            .setUv(u3, v3)
            .setColor(color3)
        consumer.addVertex(pose, x4, y4, 0f) // bottom right
            .setUv(u4, v4)
            .setColor(color4)
        consumer.addVertex(pose, x2, y2, 0f) // top right
            .setUv(u2, v2)
            .setColor(color2)
    }

    override fun pushMatrix() {
        this.matrixStack.pushMatrix()
    }

    override fun set(matrix: Matrix3x2fc) {
        this.matrixStack.set(matrix)
    }

    override fun peekMatrix(): Matrix3x2fc {
        return this.matrixStack
    }

    override fun translate(x: Float, y: Float) {
        this.matrixStack.translate(x, y)
    }

    override fun rotate(degrees: Float) {
        this.matrixStack.rotate(degrees * Mth.DEG_TO_RAD)
    }

    override fun scale(x: Float, y: Float) {
        this.matrixStack.scale(x, y)
    }

    override fun popMatrix() {
        this.matrixStack.popMatrix()
    }

    fun flushLastLayer() {
        this.awtRenderer.flushLast(this)
    }
}

