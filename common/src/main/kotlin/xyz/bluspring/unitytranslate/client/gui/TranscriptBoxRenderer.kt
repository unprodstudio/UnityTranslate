package xyz.bluspring.unitytranslate.client.gui

import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.api.v2.client.gui.UIElement
import xyz.bluspring.unitytranslate.api.v2.client.util.ScreenRectangle
import xyz.bluspring.unitytranslate.client.ClientPlatformProxy
import xyz.bluspring.unitytranslate.client.config.TranscriptBoxConfig
import xyz.bluspring.unitytranslate.client.gui.hud.TranscriptBoxContainer

class TranscriptBoxRenderer : UIElement() {
    val containers: Collection<TranscriptBoxContainer>
        get() {
            return this.children.filterIsInstance<TranscriptBoxContainer>()
        }

    override fun bounds(
        screenWidth: Int,
        screenHeight: Int
    ): ScreenRectangle {
        return ScreenRectangle(0, 0, ClientPlatformProxy.instance.viewportWidth, ClientPlatformProxy.instance.viewportHeight)
    }

    override fun tick() {
        for (container in this.containers) {
            container.tick()
        }
    }

    fun updateConfig(boxes: Collection<TranscriptBoxConfig>) {
        val existing = this.containers.toMutableList()

        // Remove boxes that no longer exist
        for (container in existing.filter { !boxes.contains(it.config) }) {
            this.removeChild(container)
            existing.remove(container)
        }

        // Add new boxes
        for (config in boxes) {
            if (existing.none { e -> e.config == config }) {
                this.addChild(TranscriptBoxContainer(UnityTranslateApi.instance.getOrCreateTranscriptHolder(config.language), config))
            }
        }

        // Now update everyone
        for (container in this.containers) {
            container.updateConfig()
        }
    }
}
