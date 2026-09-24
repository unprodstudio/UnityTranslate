package xyz.bluspring.unitytranslate.minecraft

import net.minecraft.client.input.KeyEvent

//? if >= 26.3 {
val KeyEvent.scancode: Int
    get() = this.keycode()
//? }
