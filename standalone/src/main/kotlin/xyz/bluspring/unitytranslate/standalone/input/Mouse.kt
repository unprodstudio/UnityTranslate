package xyz.bluspring.unitytranslate.standalone.input

import com.mojang.blaze3d.Blaze3D
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.input.MouseButtonInfo
import xyz.bluspring.unitytranslate.standalone.UnityTranslateStandalone

object Mouse {
    var x: Double = 0.0
    var y: Double = 0.0

    private var activeButton: MouseButtonInfo? = null
    private var mousePressedTime = -1.0

    fun cursorEntered() {

    }

    fun onMove(handle: Long, x: Double, y: Double) {
        if (handle == UnityTranslateStandalone.window.handle()) {
            this.x = x
            this.y = y
        }
    }

    fun onPress(handle: Long, info: MouseButtonInfo, action: Int) {
        if (handle == UnityTranslateStandalone.window.handle()) {
            val pressed = action == InputConstants.PRESS

            val actualInfo = this.simulateRightClick(info, pressed)
            if (pressed) {
                this.activeButton = actualInfo
                this.mousePressedTime = Blaze3D.getTime()
            } else if (this.activeButton != null) {
                this.activeButton = null
            }

            if (pressed) {
                UnityTranslateStandalone.screen.mouseClicked(this.x, this.y, info.button)
            } else {
                UnityTranslateStandalone.screen.mouseReleased(this.x, this.y, info.button)
            }
        }
    }

    fun onScroll(handle: Long, scrollX: Double, scrollY: Double) {
        if (handle == UnityTranslateStandalone.window.handle()) {
            UnityTranslateStandalone.screen.mouseScrolled(this.x, this.y, scrollX, scrollY)
        }
    }

    fun simulateRightClick(info: MouseButtonInfo, pressed: Boolean): MouseButtonInfo {
        // TODO: do we need this?
        return info
    }
}
