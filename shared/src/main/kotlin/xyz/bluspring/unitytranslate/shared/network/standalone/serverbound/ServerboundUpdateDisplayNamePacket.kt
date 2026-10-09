package xyz.bluspring.unitytranslate.shared.network.standalone.serverbound

import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.unitytranslate.shared.network.SharedNetworkPacket

@JvmRecord
data class ServerboundUpdateDisplayNamePacket(
    val name: String,
) : SharedNetworkPacket {
    companion object {
        const val PACKET_ID = "update_display_name/serverbound"

        @JvmField val CODEC = CompositeCodecs.composite(
            NetworkCodecs.STRING_UTF8, ServerboundUpdateDisplayNamePacket::name,
            ::ServerboundUpdateDisplayNamePacket
        )
    }
}
