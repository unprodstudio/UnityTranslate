package xyz.bluspring.unitytranslate.shared.network.standalone

import xyz.bluspring.modernnetworking.api.v2.packet.registry.DefaultedPacketRegistry
import xyz.bluspring.unitytranslate.shared.Constants
import xyz.bluspring.unitytranslate.shared.network.common.UnityTranslateCommonPackets
import xyz.bluspring.unitytranslate.shared.network.standalone.clientbound.ClientboundUpdateDisplayNamePacket
import xyz.bluspring.unitytranslate.shared.network.standalone.serverbound.ServerboundUpdateDisplayNamePacket

object UnityTranslateStandalonePackets {
    val clientboundRegistry = DefaultedPacketRegistry().namespaced(Constants.MOD_ID)
    val serverboundRegistry = DefaultedPacketRegistry().namespaced(Constants.MOD_ID)

    init {
        for (definition in UnityTranslateCommonPackets.clientboundRegistry.definitions) {
            clientboundRegistry.register(definition)
        }

        for (definition in UnityTranslateCommonPackets.serverboundRegistry.definitions) {
            serverboundRegistry.register(definition)
        }
    }

    // Clientbound
    val UPDATE_DISPLAY_NAME_CLIENT = clientboundRegistry.register(ClientboundUpdateDisplayNamePacket.PACKET_ID, ClientboundUpdateDisplayNamePacket.CODEC)

    // Serverbound
    val UPDATE_DISPLAY_NAME_SERVER = serverboundRegistry.register(ServerboundUpdateDisplayNamePacket.PACKET_ID, ServerboundUpdateDisplayNamePacket.CODEC)
}
