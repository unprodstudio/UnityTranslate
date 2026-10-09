package xyz.bluspring.unitytranslate.shared.network.common.clientbound

import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.unitytranslate.shared.network.SharedNetworkPacket

@JvmRecord
data class NegotiationPacket(
    val maxProtocolVersion: Int,
) : SharedNetworkPacket {
    companion object {
        const val PACKET_ID = "negotiate"

        val CODEC = CompositeCodecs.composite(
            NetworkCodecs.VAR_INT, NegotiationPacket::maxProtocolVersion,
            ::NegotiationPacket
        )
    }
}
