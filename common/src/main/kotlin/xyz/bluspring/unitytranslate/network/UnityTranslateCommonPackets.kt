package xyz.bluspring.unitytranslate.network

import xyz.bluspring.modernnetworking.api.v2.packet.registry.DefaultedPacketRegistry
import xyz.bluspring.unitytranslate.UnityTranslate
import xyz.bluspring.unitytranslate.shared.network.WrappedSharedNetworkPacket.Companion.register
import xyz.bluspring.unitytranslate.shared.network.common.clientbound.NegotiationPacket
import xyz.bluspring.unitytranslate.shared.network.common.serverbound.ClientInfoPacket
import xyz.bluspring.unitytranslate.shared.network.common.serverbound.UpdateLanguagesPacket
import xyz.bluspring.unitytranslate.shared.network.common.serverbound.UpdateTranscriptPacket

object UnityTranslateCommonPackets {
    val clientboundRegistry = DefaultedPacketRegistry().namespaced(UnityTranslate.MOD_ID)
    val serverboundRegistry = DefaultedPacketRegistry().namespaced(UnityTranslate.MOD_ID)

    // Clientbound
    val NEGOTIATE = clientboundRegistry.register(NegotiationPacket.PACKET_ID, NegotiationPacket.CODEC)

    // Serverbound
    val CLIENT_INFO = serverboundRegistry.register(ClientInfoPacket.PACKET_ID, ClientInfoPacket.CODEC)
    val UPDATE_LANGUAGES = serverboundRegistry.register(UpdateLanguagesPacket.PACKET_ID, UpdateLanguagesPacket.CODEC)
    val UPDATE_TRANSCRIPT = serverboundRegistry.register(UpdateTranscriptPacket.PACKET_ID, UpdateTranscriptPacket.CODEC)
}
