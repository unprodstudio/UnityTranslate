package xyz.bluspring.unitytranslate.shared.network.standalone.clientbound

import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.unitytranslate.shared.network.SharedNetworkPacket
import java.util.*

@JvmRecord
data class ClientboundUpdateDisplayNamePacket(
    val uuid: UUID,
    val name: String,
) : SharedNetworkPacket {
    companion object {
        const val PACKET_ID = "update_display_name/clientbound"

        @JvmField val CODEC = CompositeCodecs.composite(
            NetworkCodecs.UUID, ClientboundUpdateDisplayNamePacket::uuid,
            NetworkCodecs.STRING_UTF8, ClientboundUpdateDisplayNamePacket::name,
            ::ClientboundUpdateDisplayNamePacket
        )
    }
}
