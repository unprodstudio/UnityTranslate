package xyz.bluspring.unitytranslate.client.renderer.ui.minecraft

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import org.joml.Matrix3x2fc
import xyz.bluspring.unitytranslate.client.renderer.RenderPipeline

data class GradientedMeshFillRenderState(
    private val pipeline: RenderPipeline,
    private val textureSetup: TextureSetup,
    private val pose: Matrix3x2fc,
    private val scissorArea: ScreenRectangle?,

    val x0: Float, val y0: Float,
    val x1: Float, val y1: Float,
    val x2: Float, val y2: Float,
    val x3: Float, val y3: Float,

    val color0: Int, val color1: Int,
    val color2: Int, val color3: Int,
) : GuiElementRenderState {
    private val bounds: ScreenRectangle?

    init {
        val allX = listOf(x0, x1, x2, x3)
        val allY = listOf(y0, y1, y2, y3)
        val lowestX = allX.min()
        val lowestY = allY.min()
        val highestX = allX.max()
        val highestY = allY.max()

        this.bounds = ScreenRectangle(lowestX.toInt(), lowestY.toInt(), (highestX - lowestX).toInt(), (highestY - lowestY).toInt())
            .transformMaxBounds(this.pose)
            .run {
                if (scissorArea != null)
                    scissorArea.intersection(this)
                else
                    this
            }
    }

    override fun buildVertices(consumer: VertexConsumer) {
        consumer.addVertexWith2DPose(this.pose, this.x0, this.y0) // top left
            .setColor(this.color0)
        consumer.addVertexWith2DPose(this.pose, this.x2, this.y2) // bottom left
            .setColor(this.color2)
        consumer.addVertexWith2DPose(this.pose, this.x3, this.y3) // bottom right
            .setColor(this.color3)
        consumer.addVertexWith2DPose(this.pose, this.x1, this.y1) // top right
            .setColor(this.color1)
    }

    override fun pipeline(): RenderPipeline = this.pipeline
    override fun textureSetup(): TextureSetup = this.textureSetup
    override fun scissorArea(): ScreenRectangle? = this.scissorArea
    override fun bounds(): ScreenRectangle? = this.bounds
}
