package xyz.bluspring.unitytranslate.api.v2.client.gui.element

import org.joml.Math.clamp
import xyz.bluspring.unitytranslate.api.v2.client.InputValue
import xyz.bluspring.unitytranslate.api.v2.client.InputValue.Companion.eq
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIElement
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIGraphics
import xyz.bluspring.unitytranslate.api.v2.client.gui.font.FontReference
import xyz.bluspring.unitytranslate.api.v2.client.theme.ThemeConfig
import xyz.bluspring.unitytranslate.api.v2.client.util.ScreenRectangle
import xyz.bluspring.unitytranslate.api.v2.config.ColorConfig
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.api.v2.util.ARGBHelper.multiplyAlpha
import kotlin.math.round
import kotlin.reflect.KMutableProperty
import kotlin.reflect.typeOf

class SliderElement<T : Number>(
    val x: Float, val y: Float,
    val width: Float, val height: Float,

    val property: KMutableProperty<T>,
    min: T, max: T, step: T,
    val font: FontReference,
    val visualizer: (T) -> TextComponent,
) : UIElement(), FocusableUIElement, FadeableUIElement {
    override var opacity = 1f

    private val min = min.toFloat()
    private val max = max.toFloat()
    private val step = step.toFloat()

    override var isFocused: Boolean = false

    var value: Float
        get() = this.property.getter.call().toFloat()
        set(value) = this.property.setter.call(when (this.property.getter.returnType) {
            typeOf<Int>() -> value.toInt()
            typeOf<Float>() -> value
            typeOf<Double>() -> value.toDouble()
            typeOf<Byte>() -> value.toInt().toByte()
            typeOf<Short>() -> value.toInt().toShort()
            typeOf<Long>() -> value.toLong()
            else -> throw IllegalArgumentException("Unsupported number type ${this.property.getter.returnType}!")
        })

    private val normalizedValue: Float
        get() {
            val value = this.value
            val range = this.max - this.min
            return (value - this.min) / range
        }

    override fun bounds(screenWidth: Int, screenHeight: Int): ScreenRectangle {
        return ScreenRectangle(this.x.toInt(), this.y.toInt(), this.width.toInt(), this.height.toInt())
    }

    override fun submit(graphics: UIGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        super.submit(graphics, partialTick, mouseX, mouseY)

        if (this.isSelected) {
            this.value = this.calculateCurrentValue(mouseX)
        }

        val isFocused = this.isFocused || this.isSelected
        val normalized = this.normalizedValue

        val trackMatrix = ColorConfig.separateMatrix(if (isFocused) ThemeConfig.sliderTrackFocused else ThemeConfig.sliderTrack)
        val trackFillMatrix = ColorConfig.separateMatrix(ThemeConfig.sliderTrackFocused)
        graphics.outline(this.x, this.y + 3, this.x + this.width, this.y + this.height - 3, 1f,
            trackMatrix.topLeft.multiplyAlpha(this.opacity), trackMatrix.topRight.multiplyAlpha(this.opacity),
            trackMatrix.bottomLeft.multiplyAlpha(this.opacity), trackMatrix.bottomRight.multiplyAlpha(this.opacity)
        )

        graphics.fill(this.x + 2, this.y + 3 + 2, this.x + ((this.width - 1) * normalized), this.y + this.height - 3 - 2,
            trackFillMatrix.topLeft.multiplyAlpha(this.opacity), trackFillMatrix.topRight.multiplyAlpha(this.opacity),
            trackFillMatrix.bottomLeft.multiplyAlpha(this.opacity), trackFillMatrix.bottomRight.multiplyAlpha(this.opacity)
        )

        val notchMatrix = ColorConfig.separateMatrix(if (isFocused) ThemeConfig.sliderNotchFocused else ThemeConfig.sliderNotch)
        graphics.fill(this.x - 1 + (this.width * normalized), this.y, this.x + (this.width * normalized) + 1, this.y + this.height,
            notchMatrix.topLeft.multiplyAlpha(this.opacity), notchMatrix.topRight.multiplyAlpha(this.opacity),
            notchMatrix.bottomLeft.multiplyAlpha(this.opacity), notchMatrix.bottomRight.multiplyAlpha(this.opacity)
        )

        graphics.text(this.font, this.visualizer(this.property.getter.call()), this.x + this.width + 4, this.y + (this.height / 2f) - 3,
            (if (isFocused) ThemeConfig.sliderValueFocused else ThemeConfig.sliderValue).multiplyAlpha(this.opacity), false)

        this.isFocused = this.bounds().containsPoint(mouseX, mouseY)
    }

    private fun calculateCurrentValue(mouseX: Int): Float {
        val step = this.step / (this.max.minus(this.min))
        val delta = clamp(0f, 1f, (mouseX - this.x) / this.width)
        return this.min + (round(delta / step) * step) * (this.max - this.min)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button eq InputValue.MOUSE_BUTTON_LEFT && this.bounds().containsPoint(mouseX.toInt(), mouseY.toInt())) {
            this.value = this.calculateCurrentValue(mouseX.toInt())
            this.isSelected = true
            return true
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button eq InputValue.MOUSE_BUTTON_LEFT && this.isSelected) {
            this.isSelected = false
            return true
        }

        return super.mouseReleased(mouseX, mouseY, button)
    }
}
