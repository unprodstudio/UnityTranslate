package xyz.bluspring.unitytranslate.api.v2.client.gui.element

import kotlinx.coroutines.*
import org.joml.Math
import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.client.InputValue
import xyz.bluspring.unitytranslate.api.v2.client.InputValue.Companion.eq
import xyz.bluspring.unitytranslate.api.v2.client.InputValue.Companion.fromPlatform
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIElement
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIGraphics
import xyz.bluspring.unitytranslate.api.v2.client.gui.font.FontReference
import xyz.bluspring.unitytranslate.api.v2.client.theme.ThemeConfig
import xyz.bluspring.unitytranslate.api.v2.client.util.ScreenRectangle
import xyz.bluspring.unitytranslate.api.v2.config.ColorConfig
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.api.v2.event.Event
import xyz.bluspring.unitytranslate.api.v2.util.ARGBHelper.multiplyAlpha
import kotlin.math.floor
import kotlin.reflect.KMutableProperty

class DropdownList<E : Comparable<E>>(
    val x: Float, val y: Float,
    val width: Float, val height: Float,

    val font: FontReference,
    elements: suspend () -> Collection<E>,
    visualizer: (E) -> TextComponent,

    val property: KMutableProperty<E?>,
    val type: Type,

    validator: (E) -> Boolean = { true },
    private val tooltip: (E?) -> TextComponent = { TextComponent.empty() },
) : UIElement(), FadeableUIElement {
    fun interface DropdownCallback<E> {
        fun onDropdownEvent(item: E)
    }

    val onUpdated: Event<DropdownCallback<E?>> = Event<DropdownCallback<E?>>(DropdownCallback::class.java as Class<DropdownCallback<E?>>) { callbacks ->
        DropdownCallback<E?> {
            for (callback in callbacks) {
                callback.onDropdownEvent(it)
            }
        }
    }
    override var opacity = 1f

    constructor(x: Float, y: Float, width: Float, height: Float, font: FontReference, elements: suspend () -> Collection<E>, visualizer: (E) -> TextComponent, property: KMutableProperty<E>, validator: (E) -> Boolean = { true }, tooltip: (E?) -> TextComponent = { TextComponent.empty() },)
        : this(x, y, width, height, font, elements, visualizer, property as KMutableProperty<E?>, Type.REQUIRED, validator, tooltip)

    enum class Type {
        REQUIRED, DEFAULTED, OPTIONAL
    }

    companion object {
        private val scope = CoroutineScope(Dispatchers.Default)
    }

    private val visualizer: (E?) -> TextComponent = {
        if (it == null)
            TextComponent.translatable("unitytranslate.config.${when (this.type) {
                Type.OPTIONAL -> "none"
                Type.DEFAULTED -> "default"
                else -> throw IllegalStateException()
            }}")
        else visualizer(it)
    }

    val validator: (E?) -> Boolean = {
        if (it == null)
            this.type != Type.REQUIRED
        else
            validator(it)
    }

    private val elementGetter: Deferred<List<E>> = scope.async {
        elements().toList().sorted()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val elements: List<E?>
        get() {
            if (!this.elementGetter.isCompleted)
                return emptyList()

            val completed = this.elementGetter.getCompleted()
            return if (this.type != Type.REQUIRED)
                (completed.toMutableList() as MutableList<E?>).apply {
                    add(0, null)
                }
            else
                completed
        }
    private var currentIndex = 0
    private var lastStoredIndex = 0
    private var isOpened = false
    private var scrollOffset = 0.0

    val selected: E?
        get() {
            return this.elements[this.currentIndex]
        }

    private val mainBounds = ScreenRectangle(x.toInt(), y.toInt(), width.toInt(), height.toInt() + 1)

    override fun bounds(screenWidth: Int, screenHeight: Int): ScreenRectangle {
        return this.mainBounds
    }

    private var currentTick = 0
    private val maxScrollTick = 60
    private var isReversing = false

    private var isLoaded = false

    var isDisabled = false

    override fun tick() {
        super.tick()

        if (!this.isReversing) {
            if (this.currentTick++ >= this.maxScrollTick) {
                this.isReversing = true
            }
        } else {
            if (this.currentTick-- <= 0) {
                this.isReversing = false
            }
        }

        if (!this.isLoaded) {
            val value = this.property.getter.call()
            if (value == null && this.type != Type.REQUIRED) {
                this.currentIndex = 0
                this.lastStoredIndex = 0
            } else {
                val index = this.elements.indexOf(value)

                if (index != -1) {
                    this.currentIndex = index
                    this.lastStoredIndex = index
                } else {
                    // safeguard because i encountered a random crash
                    this.currentIndex = 0
                    this.lastStoredIndex = 0
                }
            }

            this.isLoaded = true
        }
    }

    private fun updateProperty() {
        val element = this.elements[this.currentIndex]
        this.property.setter.call(element)
        this.onUpdated.invoker().onDropdownEvent(element)
    }

    override fun submit(graphics: UIGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        super.submit(graphics, partialTick, mouseX, mouseY)

        val isHovered = this.bounds().containsPoint(mouseX, mouseY)
        val colorWithHover = if (isHovered || this.isOpened)
            ThemeConfig.textColor
        else
            ThemeConfig.textColor.multiplyAlpha(0.7255f)

//        graphics.outline(x, y, x + width, y + height, 1f, -1)
        val dropdownBgMatrix = ColorConfig.separateMatrix(ThemeConfig.dropdownBackground)
        graphics.fill(x, y, x + width, y + height,
            dropdownBgMatrix.topLeft.multiplyAlpha(this.opacity), dropdownBgMatrix.topRight.multiplyAlpha(this.opacity),
            dropdownBgMatrix.bottomLeft.multiplyAlpha(this.opacity), dropdownBgMatrix.bottomRight.multiplyAlpha(this.opacity),
        )
        graphics.fill(x, y + height, x + width, y + height + 1, colorWithHover.multiplyAlpha(this.opacity)) // underline

        val disabledColor = ThemeConfig.dropdownTextDisabled
        val isDisabled = this.isDisabled || !this.elementGetter.isCompleted || this.elements.isEmpty()

        if (!this.elementGetter.isCompleted) {
            graphics.text(this.font, TextComponent.translatable("unitytranslate.config.loading").append(".".repeat(floor(this.currentTick / 20f).toInt() + 1)), this.x + 4, this.y + (this.height / 2f - 4), disabledColor.multiplyAlpha(this.opacity), true)
        } else if (this.elements.isEmpty()) {
            graphics.text(this.font, TextComponent.translatable("unitytranslate.config.empty"), this.x + 4, this.y + (this.height / 2f - 4), disabledColor.multiplyAlpha(this.opacity), true)
        } else {
            graphics.text(this.font, ellipsize(this.visualizer(this.selected), this.width.toInt() - 15), this.x + 4, this.y + (this.height / 2f - 4), colorWithHover.multiplyAlpha(this.opacity), true)
        }

        graphics.text(this.font, TextComponent.literal(if (this.isOpened) "▲" else "▼"), this.x + this.width - 10, this.y + (this.height / 2f - 4), (if (isDisabled) disabledColor else colorWithHover).multiplyAlpha(this.opacity), true)
    }

    private fun ellipsize(text: TextComponent, maxWidth: Int): TextComponent {
        val subbed = this.font.substr(text, maxWidth)

        return if (subbed.string != text.string)
            TextComponent.literal("${subbed.string}...")
        else
            text
    }

    override fun submitLate(graphics: UIGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        super.submitLate(graphics, partialTick, mouseX, mouseY)

        var currentTooltip: TextComponent? = null

        if (this.isOpened) {
            val elements = this.elements
            val usableScreenHeight = UnityTranslateApi.instance.client.viewportHeight - this.y - this.height - 2
            val elementHeight = (this.height * elements.size)
            val maxAreaHeight = elementHeight.coerceAtMost(usableScreenHeight - 4)

            val yStart = (this.y + this.height + 2)

            val shouldShowScroll = maxAreaHeight != elementHeight

            val dropdownOpenBgMatrix = ColorConfig.separateMatrix(ThemeConfig.dropdownOpenBackground)
            graphics.flushLayer()
            graphics.fill(this.x, yStart, this.x + this.width, yStart + maxAreaHeight,
                dropdownOpenBgMatrix.topLeft.multiplyAlpha(this.opacity), dropdownOpenBgMatrix.topRight.multiplyAlpha(this.opacity),
                dropdownOpenBgMatrix.bottomLeft.multiplyAlpha(this.opacity), dropdownOpenBgMatrix.bottomRight.multiplyAlpha(this.opacity),
            )

            graphics.enableScissor(this.x.toInt(), yStart.toInt() + 1, this.width.toInt(), maxAreaHeight.toInt() - 2)
            graphics.pushMatrix()
            graphics.translate(0f, this.scrollOffset.toFloat())

            val adjustedMouseY = mouseY - this.scrollOffset.toFloat()

            for (i in elements.indices) {
                val y = yStart + (i * this.height)

                val isHovered = mouseX >= this.x && adjustedMouseY >= y && mouseX <= (this.x + this.width) && adjustedMouseY <= (y + this.height)
                val isDisabled = !this.validator(elements[i])
                val colorWithHover = if (isDisabled)
                    ThemeConfig.dropdownTextItemDisabled
                else if (isHovered)
                    ThemeConfig.dropdownTextItemHover
                else if (this.selected == elements[i])
                    ThemeConfig.dropdownTextItemSelected
                else
                    ThemeConfig.dropdownTextItem

                val text = this.visualizer(elements[i])
                val maxWidth = this.width.toInt() - 15

                if (isHovered) {
                    graphics.pushMatrix()

                    val textWidth = font.width(text)
                    if (textWidth > maxWidth) {
                        graphics.translate(-Math.lerp((this.currentTick + (partialTick * (if (this.isReversing) -1f else 1f))) / this.maxScrollTick.toFloat(), 0f, (maxWidth - textWidth).toFloat()), 0f)
                    }

                    graphics.text(this.font, text, this.x + 4, y + 4f, colorWithHover.multiplyAlpha(this.opacity), true)

                    graphics.popMatrix()

                    val tooltip = this.tooltip(elements[i])

                    if (tooltip.string.isNotEmpty()) {
                        currentTooltip = tooltip
                    }
                } else {
                    graphics.text(this.font, ellipsize(text, maxWidth), this.x + 4, y + 4f, colorWithHover.multiplyAlpha(this.opacity), true)
                }
            }

            graphics.popMatrix()
            graphics.disableScissor()

            val outlineMatrix = ColorConfig.separateMatrix(ThemeConfig.dropdownOpenOutline)
            graphics.outline(this.x, this.y + this.height + 2, this.x + this.width, this.y + this.height + 2 + maxAreaHeight, 1f,
                outlineMatrix.topLeft.multiplyAlpha(this.opacity), outlineMatrix.topRight.multiplyAlpha(this.opacity),
                outlineMatrix.bottomLeft.multiplyAlpha(this.opacity), outlineMatrix.bottomRight.multiplyAlpha(this.opacity),
            )

            if (shouldShowScroll) {
                val elementsPerHeight = maxAreaHeight / this.height
                val scrollHeight = elementsPerHeight / elements.size

                val scrollYOffset = (this.scrollOffset.toFloat() / elementHeight) * maxAreaHeight
                val scrollbarMatrix = ColorConfig.separateMatrix(ThemeConfig.scrollbar)
                graphics.fill(this.x + this.width - 1, yStart - scrollYOffset + 1, this.x + this.width, yStart - scrollYOffset + (maxAreaHeight * scrollHeight) - 1,
                    scrollbarMatrix.topLeft.multiplyAlpha(this.opacity), scrollbarMatrix.topRight.multiplyAlpha(this.opacity),
                    scrollbarMatrix.bottomLeft.multiplyAlpha(this.opacity), scrollbarMatrix.bottomRight.multiplyAlpha(this.opacity),
                )
            }
        }

        if (currentTooltip != null) {
            this.tooltip(graphics, currentTooltip)
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val elements = this.elements
        val usableScreenHeight = UnityTranslateApi.instance.client.viewportHeight - this.y - this.height - 2
        val elementHeight = (this.height * elements.size)
        val maxAreaHeight = elementHeight.coerceAtMost(usableScreenHeight - 4)

        if (this.isOpened && mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y + height + 2 && mouseY <= this.y + this.height + 2 + maxAreaHeight) {
            this.scrollOffset = Math.clamp((-elementHeight + maxAreaHeight).toDouble(), 0.0, scrollOffset + (scrollY * 9.0))
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button eq InputValue.MOUSE_BUTTON_LEFT && this.mainBounds.containsPoint(mouseX.toInt(), mouseY.toInt()) && !this.isDisabled && this.isLoaded && this.elements.isNotEmpty()) {
            this.isOpened = !this.isOpened
            this.lastStoredIndex = this.currentIndex
        } else if (button eq InputValue.MOUSE_BUTTON_LEFT && this.isOpened) {
            val elements = this.elements
            val usableScreenHeight = UnityTranslateApi.instance.client.viewportHeight - this.y - this.height - 2
            val elementHeight = (this.height * elements.size)
            val maxAreaHeight = elementHeight.coerceAtMost(usableScreenHeight - 4)

            if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y + height + 2 && mouseY <= this.y + this.height + 2 + maxAreaHeight) {
                val mouseY = mouseY - this.scrollOffset.toFloat()
                val yOffset = mouseY - (this.y + height + 2)
                val index = Math.clamp(0, elements.lastIndex, ((yOffset / elementHeight * elements.size)).toInt())

                val isDisabled = !this.validator(elements[index])

                if (!isDisabled) {
                    this.currentIndex = index
                    this.isOpened = false
                    this.updateProperty()
                }

                return true
            }

            this.isOpened = false
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
        if (this.isOpened) {
            val elements = this.elements
            val usableScreenHeight = UnityTranslateApi.instance.client.viewportHeight - this.y - this.height - 2
            val elementHeight = (this.height * elements.size)
            val maxAreaHeight = elementHeight.coerceAtMost(usableScreenHeight - 4)

            val topVisibleArea = this.y + this.height + 2 - this.scrollOffset
            val bottomVisibleArea = topVisibleArea + maxAreaHeight

            when (key.fromPlatform()) {
                InputValue.KEY_UP -> {
                    var isDisabled: Boolean
                    val startingIndex = this.currentIndex
                    do {
                        this.currentIndex--

                        if (this.currentIndex < 0)
                            this.currentIndex = this.elements.lastIndex

                        isDisabled = !this.validator(elements[this.currentIndex])

                        // safety check
                        if (startingIndex == this.currentIndex)
                            break
                    } while (isDisabled)

                    // key up moving the scroll box
                    // FIXME: balloon languages hate this for some reason.
                    val yPos = this.y + this.height + 2 + (this.currentIndex * this.height)
                    if (yPos !in topVisibleArea..bottomVisibleArea) {
                        this.scrollOffset = Math.clamp((-elementHeight + maxAreaHeight).toDouble(), 0.0, (maxAreaHeight - yPos).toDouble())
                    }

                    return true
                }

                InputValue.KEY_DOWN -> {
                    this.currentIndex++

                    if (this.currentIndex > this.elements.lastIndex)
                        this.currentIndex = 0

                    // key down moving the scroll box
                    val yPos = this.y + this.height + 2 + (this.currentIndex * this.height) + this.height
                    if (yPos !in topVisibleArea..bottomVisibleArea) {
                        this.scrollOffset = Math.clamp((-elementHeight + maxAreaHeight).toDouble(), 0.0, (maxAreaHeight - yPos + maxAreaHeight).toDouble())
                    }

                    return true
                }

                InputValue.KEY_ESCAPE -> {
                    this.isOpened = false
                    this.currentIndex = this.lastStoredIndex
                    return true
                }

                InputValue.KEY_RETURN -> {
                    this.isOpened = false
                    this.updateProperty()
                    return true
                }

                else -> {}
            }
        }

        return super.keyPressed(key, scanCode, modifiers)
    }
}
