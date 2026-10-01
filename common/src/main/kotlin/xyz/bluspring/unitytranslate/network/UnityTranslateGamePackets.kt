package xyz.bluspring.unitytranslate.network

import xyz.bluspring.modernnetworking.minecraft.api.v2.packet.MinecraftPacketRegistries
import xyz.bluspring.unitytranslate.UnityTranslate
import xyz.bluspring.unitytranslate.network.game.clientbound.PlayerTranscriptPacket

object UnityTranslateGamePackets {
    private val clientRegistry = MinecraftPacketRegistries.CLIENT_PLAY.namespaced(UnityTranslate.MOD_ID)
    private val serverRegistry = MinecraftPacketRegistries.SERVER_PLAY.namespaced(UnityTranslate.MOD_ID)

    // Clientbound
    val PLAYER_TRANSCRIPT = clientRegistry.register("transcript/player", PlayerTranscriptPacket.CODEC)
}
