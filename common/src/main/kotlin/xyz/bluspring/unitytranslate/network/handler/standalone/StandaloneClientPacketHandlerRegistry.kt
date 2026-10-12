package xyz.bluspring.unitytranslate.network.handler.standalone

import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.registry.handler.AbstractSingleReceiverPacketHandlerRegistry
import xyz.bluspring.unitytranslate.network.handler.ClientHandlerContext

object StandaloneClientPacketHandlerRegistry : AbstractSingleReceiverPacketHandlerRegistry<ClientHandlerContext>() {
    override fun <T : NetworkPacket> send(packet: T) {
        TODO("Not yet implemented")
    }
}
