package xyz.bluspring.unitytranslate.shared.network.common

import xyz.bluspring.modernnetworking.api.v2.packet.registry.DefaultedPacketRegistry
import xyz.bluspring.unitytranslate.shared.Constants
import xyz.bluspring.unitytranslate.shared.network.common.clientbound.EncryptionRequestPacket
import xyz.bluspring.unitytranslate.shared.network.common.clientbound.NegotiationPacket
import xyz.bluspring.unitytranslate.shared.network.common.clientbound.TranslatePacket
import xyz.bluspring.unitytranslate.shared.network.common.clientbound.UpdateUserTranscriptPacket
import xyz.bluspring.unitytranslate.shared.network.common.serverbound.*

object UnityTranslateCommonPackets {
    val clientboundRegistry = DefaultedPacketRegistry().namespaced(Constants.MOD_ID)
    val serverboundRegistry = DefaultedPacketRegistry().namespaced(Constants.MOD_ID)

    // Clientbound
    val ENCRYPTION_REQUEST = clientboundRegistry.register(EncryptionRequestPacket.PACKET_ID, EncryptionRequestPacket.CODEC)
    val NEGOTIATE = clientboundRegistry.register(NegotiationPacket.PACKET_ID, NegotiationPacket.CODEC)
    val TRANSLATE = clientboundRegistry.register(TranslatePacket.PACKET_ID, TranslatePacket.CODEC)
    val UPDATE_USER_TRANSCRIPT = clientboundRegistry.register(UpdateUserTranscriptPacket.PACKET_ID, UpdateUserTranscriptPacket.CODEC)

    // Serverbound
    val CLIENT_INFO = serverboundRegistry.register(ClientInfoPacket.PACKET_ID, ClientInfoPacket.CODEC)
    val ENCRYPTION_RESPONSE = serverboundRegistry.register(EncryptionResponsePacket.PACKET_ID, EncryptionResponsePacket.CODEC)
    val REQUEST_TRANSLATION = serverboundRegistry.register(RequestTranslationPacket.PACKET_ID, RequestTranslationPacket.CODEC)
    val UPDATE_LANGUAGES = serverboundRegistry.register(UpdateLanguagesPacket.PACKET_ID, UpdateLanguagesPacket.CODEC)
    val UPDATE_TRANSCRIPT = serverboundRegistry.register(UpdateTranscriptPacket.PACKET_ID, UpdateTranscriptPacket.CODEC)
}
