package xyz.bluspring.unitytranslate.api.v2.client.gui.element.context

import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.client.InputValue
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIGraphics
import xyz.bluspring.unitytranslate.api.v2.client.theme.ThemeConfig
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent

class ActionContextBoxElement(val text: TextComponent, val onClick: () -> Unit) : ContextBoxElement() {
    override fun submitElement(graphics: UIGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val font = UnityTranslateApi.instance.client.defaultFont
        graphics.centeredText(font, text, this.x + (this.width / 2f), this.y + (this.bounds().height / 2f) - (font.lineHeight / 2f),
            if (this.isFocused) ThemeConfig.contextBoxElementTextFocused else ThemeConfig.contextBoxElementText, true)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (this.bounds().containsPoint(mouseX.toInt(), mouseY.toInt()) && button == InputValue.MOUSE_BUTTON_LEFT.asPlatform) {
            this.onClick()
            return true
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }
}
