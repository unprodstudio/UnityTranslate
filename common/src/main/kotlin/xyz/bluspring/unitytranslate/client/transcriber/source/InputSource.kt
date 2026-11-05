package xyz.bluspring.unitytranslate.client.transcriber.source

import xyz.bluspring.unitytranslate.api.v2.client.AudioHelper
import xyz.bluspring.unitytranslate.api.v2.transcriber.TranscriberSource

abstract class InputSource(val source: TranscriberSource) {
    var isMuted = false

    fun tick() {
        if (this.isMuted) return

        val available = this.available
        if (available >= AudioHelper.BUFFER_SIZE) {
            val samples = this.sample()
            this.source.submitSpeechSamples(samples)
        }
    }

    protected abstract val available: Int
    protected abstract fun sample(): FloatArray

    open fun close() {}
}
