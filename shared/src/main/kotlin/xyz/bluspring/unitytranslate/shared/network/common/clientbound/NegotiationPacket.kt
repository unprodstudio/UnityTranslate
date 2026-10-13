package xyz.bluspring.unitytranslate.shared.network.common.clientbound

import io.netty.buffer.ByteBuf
import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.PacketDefinition
import xyz.bluspring.unitytranslate.shared.network.common.UnityTranslateCommonPackets

@JvmRecord
data class NegotiationPacket(
    val maxProtocolVersion: Int,
    val allowSelfTranslation: Boolean,
) : NetworkPacket {
    companion object {
        const val PACKET_ID = "negotiate"

        val CODEC = CompositeCodecs.composite(
            NetworkCodecs.VAR_INT, NegotiationPacket::maxProtocolVersion,
            NetworkCodecs.BOOL, NegotiationPacket::allowSelfTranslation,
            ::NegotiationPacket
        )
    }

    override val definition: PacketDefinition<out ByteBuf, out NetworkPacket>
        get() = UnityTranslateCommonPackets.NEGOTIATE
}
