package xyz.bluspring.unitytranslate.client.gui.screen

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import xyz.bluspring.unitytranslate.api.v2.client.gui.screen.UTScreen
import xyz.bluspring.unitytranslate.client.renderer.ui.MinecraftUIGraphics

class WrappedUTScreen(val actualScreen: UTScreen, private val parent: Screen? = null) : Screen(Component.empty()) {
    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        if (this.minecraft.level == null) {
            this.extractPanorama(graphics, a)
        }

        this.extractBlurredBackground(graphics)
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), 0)
    }

    override fun init() {
        super.init()
        this.actualScreen.setup(width, height)
    }

    override fun tick() {
        super.tick()
        actualScreen.tick()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, a)
        val g = MinecraftUIGraphics(graphics)
        actualScreen.submit(g, Minecraft.getInstance().deltaTracker.getGameTimeDeltaPartialTick(true), mouseX, mouseY)
        g.flushLastLayer()
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        return actualScreen.mouseClicked(event.x, event.y, event.button())
            || super.mouseClicked(event, doubleClick)
    }

    override fun mouseScrolled(x: Double, y: Double, scrollX: Double, scrollY: Double): Boolean {
        return actualScreen.mouseScrolled(x, y, scrollX, scrollY)
            || super.mouseScrolled(x, y, scrollX, scrollY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        return actualScreen.mouseReleased(event.x, event.y, event.button())
            || super.mouseReleased(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        return actualScreen.charTyped(event.codepoint)
            || super.charTyped(event)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        return actualScreen.keyPressed(event.key, event.scancode, event.modifiers)
            || super.keyPressed(event)
    }

    override fun keyReleased(event: KeyEvent): Boolean {
        return actualScreen.keyReleased(event.key, event.scancode, event.modifiers)
            || super.keyReleased(event)
    }

    override fun shouldCloseOnEsc(): Boolean {
        if (!actualScreen.shouldCloseOnEsc())
            return false

        return super.shouldCloseOnEsc()
    }

    override fun onClose() {
        this.minecraft.gui.setScreen(this.parent)
    }
}
