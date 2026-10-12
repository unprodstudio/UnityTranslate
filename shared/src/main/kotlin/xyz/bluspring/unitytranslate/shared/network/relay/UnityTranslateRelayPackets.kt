package xyz.bluspring.unitytranslate.shared.network.relay

import xyz.bluspring.modernnetworking.api.v2.packet.registry.DefaultedPacketRegistry
import xyz.bluspring.unitytranslate.shared.Constants
import xyz.bluspring.unitytranslate.shared.network.common.UnityTranslateCommonPackets
import xyz.bluspring.unitytranslate.shared.network.relay.clientbound.SetupConnectionPacket
import xyz.bluspring.unitytranslate.shared.network.relay.serverbound.RequestConnectionPacket

object UnityTranslateRelayPackets {
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
    val SETUP_CONNECTION = clientboundRegistry.register(SetupConnectionPacket.PACKET_ID, SetupConnectionPacket.CODEC)

    // Serverbound
    val REQUEST_CONNECTION = serverboundRegistry.register(RequestConnectionPacket.PACKET_ID, RequestConnectionPacket.CODEC)
}
