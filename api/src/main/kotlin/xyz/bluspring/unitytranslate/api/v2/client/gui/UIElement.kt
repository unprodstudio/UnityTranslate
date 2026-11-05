package xyz.bluspring.unitytranslate.api.v2.client.gui

import org.jetbrains.annotations.ApiStatus
import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.client.theme.ThemeConfig
import xyz.bluspring.unitytranslate.api.v2.client.util.ScreenRectangle
import xyz.bluspring.unitytranslate.api.v2.config.ColorConfig
import xyz.bluspring.unitytranslate.api.v2.display.text.TextComponent
import xyz.bluspring.unitytranslate.api.v2.util.ARGBHelper

abstract class UIElement {
    private val enableDebug: Boolean // don't optimize this into a const, this is so we can use hotswap to toggle debug display.
        get() = false


    protected val children: List<UIElement>
        field = mutableListOf()

    protected var isSelected = false
    protected var isInitialized = false
        private set

    @ApiStatus.NonExtendable
    open fun bounds(): ScreenRectangle {
        val access = UnityTranslateApi.instance.client
        return this.getBounds(access.viewportWidth, access.viewportHeight)
    }

    @ApiStatus.NonExtendable
    open fun getBounds(screenWidth: Int, screenHeight: Int): ScreenRectangle {
        val access = UnityTranslateApi.instance.client
        if (!this.isInitialized) {
            this.setup(access.viewportWidth, access.viewportHeight)
        }

        return this.bounds(screenWidth, screenHeight)
    }

    @ApiStatus.OverrideOnly
    protected abstract fun bounds(screenWidth: Int, screenHeight: Int): ScreenRectangle

    protected open fun init(width: Int, height: Int) {
    }

    fun setup(width: Int, height: Int) {
        this.children.clear()
        this.init(width, height)

        for (element in this.children) {
            element.setup(width, height)
        }

        this.isInitialized = true
    }

    protected fun <T : UIElement> addChild(child: T): T {
        this.children.add(child)
        return child
    }

    protected fun <T : UIElement> removeChild(child: T): T {
        this.children.remove(child)
        return child
    }

    open fun submit(graphics: UIGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        // Debug thing for showing the element bounds.
        if (this.enableDebug) {
            val bounds = this.bounds()
            val isHovered = bounds.containsPoint(mouseX, mouseY)
            val color = if (isHovered)
                ARGBHelper.color(255, 255, 255, 255)
            else
                ARGBHelper.color(60, 255, 255, 255)

            graphics.outline(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), 1f, color)

            if (isHovered) {
                val access = UnityTranslateApi.instance.client
                graphics.pushMatrix()
                graphics.translate(bounds.left.toFloat(), bounds.bottom.toFloat() + 2f)
                graphics.scale(0.5f, 0.5f)
                graphics.text(access.defaultFont, TextComponent.literal("${this::class.simpleName} (x: ${bounds.x}, y: ${bounds.y}, width: ${bounds.width}, height: ${bounds.height})"), 0f, 0f, -1, true)
                graphics.popMatrix()
            }
        }

        for (element in this.children) {
            element.submit(graphics, partialTick, mouseX, mouseY)
        }

        this.submitLate(graphics, partialTick, mouseX, mouseY)
    }

    protected open fun submitLate(graphics: UIGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        for (element in this.children) {
            element.submitLate(graphics, partialTick, mouseX, mouseY)
        }
    }

    open fun tick() {
        for (element in this.children) {
            element.tick()
        }
    }

    open fun keyPressed(key: Int, scanCode: Int, modifiers: Int): Boolean {
        for (element in this.children) {
            if (element.keyPressed(key, scanCode, modifiers))
                return true
        }

        return false
    }

    open fun keyReleased(key: Int, scanCode: Int, modifiers: Int): Boolean {
        for (element in this.children) {
            if (element.keyReleased(key, scanCode, modifiers))
                return true
        }

        return false
    }

    open fun charTyped(codepoint: Int): Boolean {
        for (element in this.children) {
            if (element.charTyped(codepoint))
                return true
        }

        return false
    }

    open fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        for (element in this.children) {
            if (element.mouseClicked(mouseX, mouseY, button))
                return true
        }

        return false
    }

    open fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        for (element in this.children) {
            if (element.mouseReleased(mouseX, mouseY, button))
                return true
        }

        return false
    }

    open fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        for (element in this.children) {
            if (element.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
                return true
        }

        return false
    }
    
    protected fun tooltip(graphics: UIGraphics, tooltip: TextComponent) {
        val access = UnityTranslateApi.instance.client
        
        val mouseX = (access.mouseX / access.guiScale).toFloat()
        val mouseY = (access.mouseY / access.guiScale).toFloat()
        val font = access.defaultFont

        val split = font.split(tooltip, 220)
        val maxWidth = split.maxOfOrNull { font.width(it) } ?: 0

        val tooltipWidth = maxWidth + 8
        var xPos = mouseX + 4f
        var yPos = mouseY - 3
        if (access.viewportWidth <= xPos + tooltipWidth)
            xPos -= (xPos + tooltipWidth) - access.viewportWidth + 4

        if (access.viewportHeight <= yPos + (split.size * font.lineHeight) + 5)
            yPos -= (yPos + (split.size * font.lineHeight) + 5) - access.viewportHeight + 4

        val bgMatrix = ColorConfig.separateMatrix(ThemeConfig.tooltipBackground)
        graphics.fill(xPos, yPos, xPos + tooltipWidth, yPos + (split.size * font.lineHeight) + 5,
            bgMatrix.topLeft, bgMatrix.topRight,
            bgMatrix.bottomRight, bgMatrix.bottomRight
        )

        for ((index, text) in split.withIndex()) {
            graphics.text(font, text, xPos + 4, yPos + 3 + (index * font.lineHeight),
                ThemeConfig.tooltipText, false)
        }
    }
}
