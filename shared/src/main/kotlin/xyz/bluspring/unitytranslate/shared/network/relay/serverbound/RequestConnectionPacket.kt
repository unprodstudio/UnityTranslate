package xyz.bluspring.unitytranslate.shared.network.relay.serverbound

import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.unitytranslate.shared.network.SharedNetworkPacket

@JvmRecord
data class RequestConnectionPacket(
    val inviteCode: String,
    val password: String,
) : SharedNetworkPacket {
    companion object {
        const val PACKET_ID = "connection/request"

        @JvmField val CODEC = CompositeCodecs.composite(
            NetworkCodecs.STRING_UTF8, RequestConnectionPacket::inviteCode,
            NetworkCodecs.STRING_UTF8, RequestConnectionPacket::password,
            ::RequestConnectionPacket
        )
    }
}
