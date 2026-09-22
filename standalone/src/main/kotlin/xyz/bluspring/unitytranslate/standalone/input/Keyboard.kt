package xyz.bluspring.unitytranslate.standalone.input

import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import xyz.bluspring.unitytranslate.standalone.UnityTranslateStandalone

object Keyboard {
    fun keyPress(handle: Long, action: Int, event: KeyEvent) {
        if (handle == UnityTranslateStandalone.window.handle()) {
            UnityTranslateStandalone.screen.keyPressed(event.key ,event.scancode, event.modifiers)
        }
    }

    fun charTyped(handle: Long, event: CharacterEvent) {
        if (handle == UnityTranslateStandalone.window.handle()) {
            UnityTranslateStandalone.screen.charTyped(event.codepoint)
        }
    }
}
