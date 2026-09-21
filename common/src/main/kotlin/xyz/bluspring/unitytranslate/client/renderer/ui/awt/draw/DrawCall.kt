package xyz.bluspring.unitytranslate.client.renderer.ui.awt.draw

import xyz.bluspring.unitytranslate.client.ClientPlatformProxy
import xyz.bluspring.unitytranslate.client.renderer.ui.awt.Layer

interface DrawCall {
    fun draw(layer: Layer) {
        this.draw(layer, guiScale)
    }

    fun draw(layer: Layer, guiScale: Float)

    companion object {
        val guiScale: Float
            get() = ClientPlatformProxy.instance.guiScale.toFloat()
    }
}
