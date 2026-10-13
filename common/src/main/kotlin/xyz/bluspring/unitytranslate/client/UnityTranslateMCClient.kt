package xyz.bluspring.unitytranslate.client

import xyz.bluspring.unitytranslate.PlatformProxy
import xyz.bluspring.unitytranslate.api.v2.UnityTranslateApi
import xyz.bluspring.unitytranslate.client.transcriber.sender.MinecraftLocalTranscriptUser
import xyz.bluspring.unitytranslate.integration.plasmo.PlasmoVoiceLoader

object UnityTranslateMCClient {
    val transcriberSource = UnityTranslateApi.instance.getOrCreateTranscriberSource(MinecraftLocalTranscriptUser)
    var serverSupportsTranslator = false

    fun init() {
        if (PlatformProxy.instance.isModLoaded("plasmovoice")) {
            PlasmoVoiceLoader.loadClient()
        }
    }

    fun onLeaveServer() {
        serverSupportsTranslator = false
    }
}
