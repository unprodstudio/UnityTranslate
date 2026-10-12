package xyz.bluspring.unitytranslate.shared.network.relay.serverbound

import io.netty.buffer.ByteBuf
import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.PacketDefinition
import xyz.bluspring.unitytranslate.shared.network.relay.UnityTranslateRelayPackets

@JvmRecord
data class RequestConnectionPacket(
    val inviteCode: String,
    val password: String,
) : NetworkPacket {
    companion object {
        const val PACKET_ID = "connection/request"

        @JvmField val CODEC = CompositeCodecs.composite(
            NetworkCodecs.STRING_UTF8, RequestConnectionPacket::inviteCode,
            NetworkCodecs.STRING_UTF8, RequestConnectionPacket::password,
            ::RequestConnectionPacket
        )
    }

    override val definition: PacketDefinition<out ByteBuf, out NetworkPacket>
        get() = UnityTranslateRelayPackets.REQUEST_CONNECTION
}
