package xyz.bluspring.unitytranslate.minecraft

import com.mojang.blaze3d.pipeline.RenderTarget
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.TitleScreen
import xyz.bluspring.unitytranslate.api.v2.client.gui.font.FontReference
import xyz.bluspring.unitytranslate.api.v2.client.gui.screen.UTScreen
import xyz.bluspring.unitytranslate.client.gui.screen.WrappedUTScreen

abstract class MinecraftClientPlatformProxy : Blaze3DClientPlatformProxy() {
    override val framebuffer: RenderTarget
        get() =
            Minecraft.getInstance().gameRenderer.mainRenderTarget()

    override val windowHandle: Long
        get() =
            Minecraft.getInstance().window.handle()

    override val shouldRenderGui: Boolean
        get() =
            !Minecraft.getInstance().gameRenderer.gameRenderState().guiRenderState.isHudHidden

    override fun setScreen(screen: UTScreen?) {
        Minecraft.getInstance().execute {
            if (screen != null) {
                Minecraft.getInstance().setScreenAndShow(WrappedUTScreen(screen))
            } else {
                if (Minecraft.getInstance().level == null) {
                    Minecraft.getInstance().gui.setScreen(TitleScreen(true))
                } else {
                    Minecraft.getInstance().gui.setScreen(null)
                }
            }
        }
    }

//    override val defaultFont: FontReference
//        get() = FontReference.minecraft(Minecraft.getInstance().font)
    override val defaultFont = FontReference.freeType(this::class.java.getResourceAsStream("/assets/unitytranslate/font/tiktok_sans.ttf")!!, 10f)

    override val renderThread: Thread
        get() = Minecraft.getInstance().runningThread
    override val mouseX: Double
        get() = Minecraft.getInstance().mouseHandler.xpos()
    override val mouseY: Double
        get() = Minecraft.getInstance().mouseHandler.ypos()
    override val windowWidth: Int
        get() = Minecraft.getInstance().window.width
    override val windowHeight: Int
        get() = Minecraft.getInstance().window.height
    override val viewportWidth: Int
        get() = Minecraft.getInstance().window.guiScaledWidth
    override val viewportHeight: Int
        get() = Minecraft.getInstance().window.guiScaledHeight
    override val guiScale: Double
        get() = Minecraft.getInstance().window.guiScale.toDouble()
}
