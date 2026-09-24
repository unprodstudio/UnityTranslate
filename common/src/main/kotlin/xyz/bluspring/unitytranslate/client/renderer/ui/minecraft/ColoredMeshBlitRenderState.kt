package xyz.bluspring.unitytranslate.client.renderer.ui.minecraft

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.TextureSetup
import net.minecraft.client.renderer.state.gui.GuiElementRenderState
import org.joml.Matrix3x2fc
import xyz.bluspring.unitytranslate.client.renderer.RenderPipeline

data class ColoredMeshBlitRenderState(
    private val pipeline: RenderPipeline,
    private val textureSetup: TextureSetup,
    private val pose: Matrix3x2fc,
    private val scissorArea: ScreenRectangle?,

    /*
    0     1
    2     3
     */
    val x0: Float, val y0: Float,
    val x1: Float, val y1: Float,
    val x2: Float, val y2: Float,
    val x3: Float, val y3: Float,

    val color0: Int, val color1: Int,
    val color2: Int, val color3: Int,

    val u0: Float, val v0: Float,
    val u1: Float, val v1: Float,
    val u2: Float, val v2: Float,
    val u3: Float, val v3: Float,
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
        /*
        (0,0)   (1,0)
        (0,1)   (1,1) -> (0,0), (0,1), (1,1), (1,0)
         */

        consumer.addVertexWith2DPose(this.pose, this.x0, this.y0) // top left
            .setUv(this.u0, this.v0)
            .setColor(this.color0)
        consumer.addVertexWith2DPose(this.pose, this.x2, this.y2) // bottom left
            .setUv(this.u2, this.v2)
            .setColor(this.color2)
        consumer.addVertexWith2DPose(this.pose, this.x3, this.y3) // bottom right
            .setUv(this.u3, this.v3)
            .setColor(this.color3)
        consumer.addVertexWith2DPose(this.pose, this.x1, this.y1) // top right
            .setUv(this.u1, this.v1)
            .setColor(this.color1)
    }

    override fun pipeline(): RenderPipeline = this.pipeline
    override fun textureSetup(): TextureSetup = this.textureSetup
    override fun scissorArea(): ScreenRectangle? = this.scissorArea
    override fun bounds(): ScreenRectangle? = this.bounds
}
