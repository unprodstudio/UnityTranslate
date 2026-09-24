package xyz.bluspring.unitytranslate.client.renderer.ui.minecraft

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import org.joml.Matrix3x2fc
import xyz.bluspring.unitytranslate.client.renderer.RenderPipeline

data class ColoredBlitRenderState(
    private val pipeline: RenderPipeline,
    private val textureSetup: TextureSetup,
    private val pose: Matrix3x2fc,
    private val scissorArea: ScreenRectangle?,

    val x0: Float, val y0: Float,
    val x1: Float, val y1: Float,

    val colorTopLeft: Int, val colorTopRight: Int,
    val colorBottomLeft: Int, val colorBottomRight: Int,

    val u0: Float, val v0: Float,
    val u1: Float, val v1: Float,
) : GuiElementRenderState {
    private val bounds: ScreenRectangle? = ScreenRectangle(x0.toInt(), y0.toInt(), (x1 - x0).toInt(), (y1 - y0).toInt())
        .transformMaxBounds(this.pose)
        .run {
            if (scissorArea != null)
                scissorArea.intersection(this)
            else
                this
        }

    override fun buildVertices(consumer: VertexConsumer) {
        consumer.addVertexWith2DPose(this.pose, this.x0, this.y0)
            .setUv(this.u0, this.v0)
            .setColor(this.colorTopLeft)
        consumer.addVertexWith2DPose(this.pose, this.x0, this.y1)
            .setUv(this.u0, this.v1)
            .setColor(this.colorBottomLeft)
        consumer.addVertexWith2DPose(this.pose, this.x1, this.y1)
            .setUv(this.u1, this.v1)
            .setColor(this.colorBottomRight)
        consumer.addVertexWith2DPose(this.pose, this.x1, this.y0)
            .setUv(this.u1, this.v0)
            .setColor(this.colorTopRight)
    }

    override fun pipeline(): RenderPipeline = this.pipeline
    override fun textureSetup(): TextureSetup = this.textureSetup
    override fun scissorArea(): ScreenRectangle? = this.scissorArea
    override fun bounds(): ScreenRectangle? = this.bounds
}
