package xyz.bluspring.unitytranslate.network

import xyz.bluspring.modernnetworking.minecraft.api.v2.packet.MinecraftPacketRegistries
import xyz.bluspring.unitytranslate.UnityTranslate
import xyz.bluspring.unitytranslate.shared.network.UnityTranslateCommonPackets

object UnityTranslateGamePackets {
    private val clientRegistry = MinecraftPacketRegistries.CLIENT_PLAY.namespaced(UnityTranslate.MOD_ID)
    private val serverRegistry = MinecraftPacketRegistries.SERVER_PLAY.namespaced(UnityTranslate.MOD_ID)

    fun adaptFromCommon() {
        for (definition in UnityTranslateCommonPackets.serverboundRegistry.definitions) {
            serverRegistry.register(definition)
        }

        for (definition in UnityTranslateCommonPackets.clientboundRegistry.definitions) {
            clientRegistry.register(definition)
        }
    }
}
