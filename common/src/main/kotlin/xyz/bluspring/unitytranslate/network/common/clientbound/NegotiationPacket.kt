package xyz.bluspring.unitytranslate.network.common.clientbound

import io.netty.buffer.ByteBuf
import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.PacketDefinition
import xyz.bluspring.unitytranslate.network.UnityTranslateCommonPackets

@JvmRecord
data class NegotiationPacket(
    val maxProtocolVersion: Int,
) : NetworkPacket {
    override val definition: PacketDefinition<out ByteBuf, out NetworkPacket>
        get() = UnityTranslateCommonPackets.NEGOTIATE

    companion object {
        val CODEC = CompositeCodecs.composite(
            NetworkCodecs.VAR_INT, NegotiationPacket::maxProtocolVersion,
            ::NegotiationPacket
        )
    }
}
