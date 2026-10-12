package xyz.bluspring.unitytranslate.network.handler.standalone

import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.registry.handler.AbstractPacketHandlerRegistry
import xyz.bluspring.unitytranslate.network.handler.ServerHandlerContext
import java.util.*

object StandaloneServerPacketHandlerRegistry : AbstractPacketHandlerRegistry<ServerHandlerContext, UUID>() {
    override fun <T : NetworkPacket> send(
        receiver: UUID,
        packet: T
    ) {
        TODO("Not yet implemented")
    }
}
