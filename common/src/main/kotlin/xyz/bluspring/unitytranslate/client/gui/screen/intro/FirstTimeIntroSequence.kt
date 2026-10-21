package xyz.bluspring.unitytranslate.client.gui.screen.intro

import net.minecraft.ChatFormatting
import net.minecraft.util.Mth
import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIGraphics
import xyz.bluspring.unitytranslate.api.v2.client.gui.element.PlainUIButton
import xyz.bluspring.unitytranslate.api.v2.client.theme.ThemeConfig
import xyz.bluspring.unitytranslate.api.v2.client.util.CommonEasing
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.api.v2.util.ARGBHelper.multiplyAlpha
import xyz.bluspring.unitytranslate.api.v2.util.ARGBHelper.withAlpha
import xyz.bluspring.unitytranslate.client.gui.LogoTransitionOverlay
import xyz.bluspring.unitytranslate.client.gui.screen.FirstStartupScreen
import xyz.bluspring.unitytranslate.util.PlatformConversion.withStyle

class FirstTimeIntroSequence(parent: FirstStartupScreen) : IntroSequence(parent) {
    override fun init(width: Int, height: Int) {
        super.init(width, height)

        this.addChild(PlainUIButton(
            width / 2f, height / 2f + 80f,
            TextComponent.translatable("unitytranslate.intro.first_time.next").withStyle(ChatFormatting.UNDERLINE),
            UnityTranslateApi.instance.client.defaultFont,
        ) {
            this.parent.next()
        })
    }

    override fun submit(graphics: UIGraphics, partialTick: Float, mouseX: Int, mouseY: Int, transitionProgress: Float) {
        val size = 80f

        graphics.pushMatrix()
        graphics.translate(graphics.width / 2f - (size / 2f), (graphics.height / 2f - (size / 2f)) - Mth.lerp(CommonEasing.SMOOTH.getValue(transitionProgress), 0f, 32f))
        graphics.blitWithColor(0f, 0f, size, size, 0f, 0f, 1f, 1f, LogoTransitionOverlay.logoTexture, if (this.isReversed)
            (-1).withAlpha(CommonEasing.SMOOTH.getValue(transitionProgress))
        else -1)
        graphics.popMatrix()

        val font = UnityTranslateApi.instance.client.defaultFont
        val splitText = font.split(TextComponent.translatable("unitytranslate.intro.first_time"), 315)

        graphics.pushMatrix()
        graphics.translate(0f, Mth.lerp(1f - CommonEasing.SMOOTH.getValue(transitionProgress), 0f, 4f))
        for ((index, text) in splitText.withIndex()) {
            graphics.centeredText(font, text,
                graphics.width / 2f, graphics.height / 2f + 40f + (index * font.lineHeight),
                ThemeConfig.textColor.multiplyAlpha(transitionProgress), true)
        }
        graphics.popMatrix()
    }
}
