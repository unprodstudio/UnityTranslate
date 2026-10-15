package xyz.bluspring.unitytranslate.api.v2.client

import xyz.bluspring.unitytranslate.api.v2.client.gui.font.FontReference
import xyz.bluspring.unitytranslate.api.v2.client.gui.screen.UTScreen

interface ClientAccess {
    val guiScale: Double
    val mouseX: Double
    val mouseY: Double

    val windowWidth: Int
    val windowHeight: Int

    fun setScreen(screen: UTScreen?)

    /**
     * Translates an [InputValue] to the current platform's input value.
     */
    fun translate(value: InputValue): Int

    /**
     * Translates the current platform's input value to an [InputValue].
     */
    fun translate(value: Int): InputValue?

    /**
     * Represents the active language code being used for display in UIs.
     */
    val activeLanguageCode: String

    /**
     * Represents the width of the viewport, which is the window width divided by the GUI scale.
     */
    val viewportWidth: Int
    /**
     * Represents the height of the viewport, which is the window height divided by the GUI scale.
     */
    val viewportHeight: Int

    /**
     * Gets the default font used in this instance of UnityTranslate.
     */
    val defaultFont: FontReference
}
