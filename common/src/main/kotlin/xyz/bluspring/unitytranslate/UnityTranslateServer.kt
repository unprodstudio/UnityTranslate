package xyz.bluspring.unitytranslate

import xyz.bluspring.unitytranslate.network.handler.UnityTranslateNetworkHandlers
import xyz.bluspring.unitytranslate.shared.network.UnityTranslateCommonPackets

object UnityTranslateServer {
    fun init() {
        UnityTranslateNetworkHandlers.SERVER.register(UnityTranslateCommonPackets.CLIENT_INFO) { packet, ctx ->

        }
    }
}
