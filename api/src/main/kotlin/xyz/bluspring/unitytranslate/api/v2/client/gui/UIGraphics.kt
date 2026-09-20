package xyz.bluspring.unitytranslate.api.v2.client.gui

import org.joml.Matrix3x2fc
import xyz.bluspring.unitytranslate.api.v2.client.gui.font.FontReference
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.api.v2.util.ARGBHelper
import xyz.bluspring.unitytranslate.api.v2.util.ColorMatrix

/**
 * An abstraction that allows drawing to a screen, designed to be similar to Minecraft's GuiGraphics, except with a few more additional useful things.
 */
interface UIGraphics {
    val width: Int
    val height: Int
    fun enableScissor(x: Int, y: Int, width: Int, height: Int)
    fun disableScissor()

    fun centeredText(font: FontReference, text: TextComponent, x: Float, y: Float, color: Int, dropShadow: Boolean)
        = text(font, text, x - (font.width(text) / 2f), y, color, dropShadow)

    fun text(font: FontReference, text: TextComponent, x: Float, y: Float, color: Int, dropShadow: Boolean)

    fun fill(x1: Float, y1: Float, x2: Float, y2: Float, colorFrom: Int, colorTo: Int = colorFrom)
        = fill(x1, y1, x2, y2, colorFrom, colorFrom, colorTo, colorTo)

    fun fill(x1: Float, y1: Float, x2: Float, y2: Float, matrix: ColorMatrix)
        = fill(x1, y1, x2, y2, matrix.topLeft, matrix.topRight, matrix.bottomLeft, matrix.bottomRight)

    fun fill(x1: Float, y1: Float, x2: Float, y2: Float, colorTopLeft: Int, colorTopRight: Int, colorBottomLeft: Int, colorBottomRight: Int)

    fun meshFill(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float, color1: Int, color2: Int = color1, color3: Int = color1, color4: Int = color1)
    fun meshFill(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float, matrix: ColorMatrix)
        = meshFill(x1, y1, x2, y2, x3, y3, x4, y4, matrix.topLeft, matrix.topRight, matrix.bottomLeft, matrix.bottomRight)

    fun outline(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float = 1f, colorFrom: Int, colorTo: Int = colorFrom)
        = outline(x1, y1, x2, y2, thickness, colorFrom, colorFrom, colorTo, colorTo)

    fun outline(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float = 1f, matrix: ColorMatrix)
        = outline(x1, y1, x2, y2, thickness, matrix.topLeft, matrix.topRight, matrix.bottomLeft, matrix.bottomRight)

    fun outline(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float = 1f, colorTopLeft: Int, colorTopRight: Int, colorBottomLeft: Int, colorBottomRight: Int) {
        val width = x2 - x1
        val height = y2 - y1

        // these names are confusing
        /*
        O - outer
        I - inner

        O          O   - upper
          I      I     - lower

          I      I     - upper
        O          O   - lower
         */
        val colorOuterLowerTopLeft = ARGBHelper.matrixSrgbLerp(colorTopLeft, colorTopRight, colorBottomLeft, colorBottomRight, 0f, thickness / height)
        val colorInnerLowerTopLeft = ARGBHelper.matrixSrgbLerp(colorTopLeft, colorTopRight, colorBottomLeft, colorBottomRight, thickness / width, thickness / height)
        val colorInnerLowerTopRight = ARGBHelper.matrixSrgbLerp(colorTopLeft, colorTopRight, colorBottomLeft, colorBottomRight, (width - thickness) / width, thickness / height)
        val colorOuterLowerTopRight = ARGBHelper.matrixSrgbLerp(colorTopLeft, colorTopRight, colorBottomLeft, colorBottomRight, 1f, thickness / height)

        val colorOuterUpperBottomLeft = ARGBHelper.matrixSrgbLerp(colorTopLeft, colorTopRight, colorBottomLeft, colorBottomRight, 0f, (height - thickness) / height)
        val colorInnerUpperBottomLeft = ARGBHelper.matrixSrgbLerp(colorTopLeft, colorTopRight, colorBottomLeft, colorBottomRight, thickness / width, (height - thickness) / height)
        val colorInnerUpperBottomRight = ARGBHelper.matrixSrgbLerp(colorTopLeft, colorTopRight, colorBottomLeft, colorBottomRight, (width - thickness) / width, (height - thickness) / height)
        val colorOuterUpperBottomRight = ARGBHelper.matrixSrgbLerp(colorTopLeft, colorTopRight, colorBottomLeft, colorBottomRight, 1f, (height - thickness) / height)

        this.fill(x1, y1, x2, y1 + thickness, colorTopLeft, colorTopRight, colorOuterLowerTopLeft, colorOuterLowerTopRight) // top
        this.fill(x1, y2 - thickness, x2, y2, colorOuterUpperBottomLeft, colorOuterUpperBottomRight, colorBottomLeft, colorBottomRight) // bottom

        this.fill(x1, y1 + thickness, x1 + thickness, y2 - thickness, colorOuterLowerTopLeft, colorInnerLowerTopLeft, colorOuterUpperBottomLeft, colorInnerUpperBottomLeft) // left
        this.fill(x2 - thickness, y1 + thickness, x2, y2 - thickness, colorInnerLowerTopRight, colorOuterLowerTopRight, colorInnerUpperBottomRight, colorOuterUpperBottomRight) // right
    }

    fun meshBlit(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float,
                          u1: Float, v1: Float, u2: Float, v2: Float, u3: Float, v3: Float, u4: Float, v4: Float, texture: TextureReference)
        = this.meshBlitWithColor(x1, y1, x2, y2, x3, y3, x4, y4, u1, v1, u2, v2, u3, v3, u4, v4, texture, -1)

    fun meshBlitWithColor(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float,
                          u1: Float, v1: Float, u2: Float, v2: Float, u3: Float, v3: Float, u4: Float, v4: Float, texture: TextureReference, matrix: ColorMatrix)
        = this.meshBlitWithColor(x1, y1, x2, y2, x3, y3, x4, y4, u1, v1, u2, v2, u3, v3, u4, v4, texture, matrix.topLeft, matrix.topRight, matrix.bottomLeft, matrix.bottomRight)

    fun meshBlitWithColor(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, x4: Float, y4: Float,
                 u1: Float, v1: Float, u2: Float, v2: Float, u3: Float, v3: Float, u4: Float, v4: Float, texture: TextureReference,
                 color1: Int, color2: Int = color1, color3: Int = color1, color4: Int = color1)

    fun blit(x1: Float, y1: Float, x2: Float, y2: Float, u0: Float, v0: Float, u1: Float, v1: Float, texture: TextureReference)
        = this.blitWithColor(x1, y1, x2, y2, u0, v0, u1, v1, texture, -1, -1)
    fun blitWithColor(x1: Float, y1: Float, x2: Float, y2: Float, u0: Float, v0: Float, u1: Float, v1: Float, texture: TextureReference, colorFrom: Int, colorTo: Int = colorFrom)
        = this.blitWithColor(x1, y1, x2, y2, u0, v0, u1, v1, texture, colorFrom, colorFrom, colorTo, colorTo)

    fun blitWithColor(x1: Float, y1: Float, x2: Float, y2: Float, u0: Float, v0: Float, u1: Float, v1: Float, texture: TextureReference, matrix: ColorMatrix)
        = this.blitWithColor(x1, y1, x2, y2, u0, v0, u1, v1, texture, matrix.topLeft, matrix.topRight, matrix.bottomLeft, matrix.bottomRight)

    fun blitWithColor(x1: Float, y1: Float, x2: Float, y2: Float, u0: Float, v0: Float, u1: Float, v1: Float, texture: TextureReference, colorTopLeft: Int, colorTopRight: Int, colorBottomLeft: Int, colorBottomRight: Int)

    fun pushMatrix()
    fun set(matrix: Matrix3x2fc)
    fun translate(x: Float, y: Float)
    fun rotate(degrees: Float)
    fun scale(x: Float, y: Float)
    fun popMatrix()
}
