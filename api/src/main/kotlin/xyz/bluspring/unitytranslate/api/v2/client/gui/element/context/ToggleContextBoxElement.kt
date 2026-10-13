package xyz.bluspring.unitytranslate.api.v2.client.gui.element.context

import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.client.InputValue
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIGraphics
import xyz.bluspring.unitytranslate.api.v2.client.theme.ThemeConfig
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import kotlin.reflect.KMutableProperty

class ToggleContextBoxElement(
    val property: KMutableProperty<Boolean>,
    val rootKey: String,
) : ContextBoxElement() {
    override fun submitElement(graphics: UIGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val font = UnityTranslateApi.instance.client.defaultFont
        val value = this.property.getter.call()
        val text = TextComponent.translatable(this.rootKey).append(": ").append(
            TextComponent.translatable("unitytranslate.value.$value")
                .withStyle { style -> style.withColor(if (value) ThemeConfig.enabledText else ThemeConfig.disabledText) }
        )
        graphics.centeredText(font, text, this.x + (this.width / 2f), this.y + (this.bounds().height / 2f) - (font.lineHeight / 2f),
            if (this.isFocused) ThemeConfig.contextBoxElementTextFocused else ThemeConfig.contextBoxElementText, true)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (this.bounds().containsPoint(mouseX.toInt(), mouseY.toInt())) {
            if (button == InputValue.MOUSE_BUTTON_LEFT.asPlatform || button == InputValue.MOUSE_BUTTON_RIGHT.asPlatform) {
                this.property.setter.call(!this.property.getter.call())
            }

            return true
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }
}
