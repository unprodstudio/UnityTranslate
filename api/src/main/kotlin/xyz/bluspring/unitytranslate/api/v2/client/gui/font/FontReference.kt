package xyz.bluspring.unitytranslate.api.v2.client.gui.font

import net.minecraft.client.gui.Font
import org.jetbrains.annotations.ApiStatus
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import java.io.InputStream

/**
 * A reference to the current font used.
 */
interface FontReference {
    val lineHeight: Int

    fun width(text: TextComponent): Int
    fun width(text: String): Int
    fun split(text: TextComponent, maxWidth: Int): List<TextComponent>
    fun substr(text: TextComponent, maxWidth: Int): TextComponent

    companion object {
        @JvmStatic @ApiStatus.Experimental
        fun minecraft(font: Font): FontReference = Class.forName("xyz.bluspring.unitytranslate.client.renderer.ui.font.SmoothFontReference")
            .getConstructor(Font::class.java)
            .newInstance(font) as FontReference

        @JvmStatic
        fun smooth(fontStream: InputStream, size: Float): FontReference = Class.forName("xyz.bluspring.unitytranslate.client.renderer.ui.font.SmoothFontReference")
            .getConstructor(InputStream::class.java, Float::class.java)
            .newInstance(fontStream, size) as FontReference
    }
}
