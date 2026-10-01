package xyz.bluspring.unitytranslate.network

import xyz.bluspring.modernnetworking.api.v2.packet.registry.DefaultedPacketRegistry
import xyz.bluspring.unitytranslate.UnityTranslate
import xyz.bluspring.unitytranslate.network.common.clientbound.NegotiationPacket
import xyz.bluspring.unitytranslate.network.common.serverbound.ClientInfoPacket
import xyz.bluspring.unitytranslate.network.common.serverbound.UpdateLanguagesPacket
import xyz.bluspring.unitytranslate.network.common.serverbound.UpdateTranscriptPacket

object UnityTranslateCommonPackets {
    val clientboundRegistry = DefaultedPacketRegistry().namespaced(UnityTranslate.MOD_ID)
    val serverboundRegistry = DefaultedPacketRegistry().namespaced(UnityTranslate.MOD_ID)

    // Clientbound
    val NEGOTIATE = clientboundRegistry.register("negotiate", NegotiationPacket.CODEC)

    // Serverbound
    val CLIENT_INFO = serverboundRegistry.register("client_info", ClientInfoPacket.CODEC)
    val UPDATE_LANGUAGES = serverboundRegistry.register("update_languages", UpdateLanguagesPacket.CODEC)
    val UPDATE_TRANSCRIPT = serverboundRegistry.register("update_transcript", UpdateTranscriptPacket.CODEC)
}
