package xyz.bluspring.unitytranslate.client.gui

import net.minecraft.util.Mth
import xyz.bluspring.unitytranslate.UnityTranslate
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIGraphics
import xyz.bluspring.unitytranslate.api.v2.client.theme.ThemeConfig
import xyz.bluspring.unitytranslate.api.v2.client.util.CommonEasing
import xyz.bluspring.unitytranslate.api.v2.config.ColorConfig
import xyz.bluspring.unitytranslate.api.v2.util.ARGBHelper
import xyz.bluspring.unitytranslate.api.v2.util.ARGBHelper.multiplyAlpha
import xyz.bluspring.unitytranslate.client.ClientPlatformProxy
import xyz.bluspring.unitytranslate.client.gui.screen.FirstStartupScreen
import xyz.bluspring.unitytranslate.client.renderer.ui.texture.MinecraftTextureReference
import kotlin.math.max

object LogoTransitionOverlay {
    const val TRANSITION_TIME = 30 // ticks
    val logoTexture = MinecraftTextureReference(UnityTranslate.id("textures/gui/icon_transparent.png"))

    var currentTick = -1

    fun tick() {
        if (this.currentTick < 0)
            return

        if (this.currentTick++ >= TRANSITION_TIME) {
            ClientPlatformProxy.instance.setScreen(FirstStartupScreen())
            this.currentTick = -1
        }
    }

    fun submit(graphics: UIGraphics, partialTick: Float) {
        if (this.currentTick < 0)
            return

        val minSize = 80f
        val maxSize = max(graphics.width, graphics.height).toFloat() + 512f
        val delta = ((this.currentTick + partialTick) / TRANSITION_TIME.toFloat()).coerceAtMost(1f)
        val size = Mth.clamp(Mth.lerp(CommonEasing.EASE_IN_CUBIC.getValue(delta), maxSize, minSize), minSize, maxSize)

        val matrix = ColorConfig.separateMatrix(ThemeConfig.mainBackground)
        graphics.fill(0f, 0f, graphics.width.toFloat(), graphics.height.toFloat(),
            matrix.topLeft.multiplyAlpha(delta), matrix.topRight.multiplyAlpha(delta),
            matrix.bottomLeft.multiplyAlpha(delta), matrix.bottomRight.multiplyAlpha(delta),
        )

        graphics.pushMatrix()
        graphics.translate(graphics.width / 2f - (size / 2f), graphics.height / 2f - (size / 2f))
        graphics.blitWithColor(0f, 0f, size, size, 0f, 0f, 1f, 1f, this.logoTexture, ARGBHelper.colorFromFloat(delta, 1f, 1f, 1f))
        graphics.popMatrix()
    }
}
