package xyz.bluspring.unitytranslate.shared.network.standalone.clientbound

import io.netty.buffer.ByteBuf
import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.PacketDefinition
import xyz.bluspring.unitytranslate.shared.network.standalone.UnityTranslateStandalonePackets
import java.util.*

@JvmRecord
data class ClientboundUpdateDisplayNamePacket(
    val uuid: UUID,
    val name: String,
) : NetworkPacket {
    companion object {
        const val PACKET_ID = "update_display_name/clientbound"

        @JvmField val CODEC = CompositeCodecs.composite(
            NetworkCodecs.UUID, ClientboundUpdateDisplayNamePacket::uuid,
            NetworkCodecs.STRING_UTF8, ClientboundUpdateDisplayNamePacket::name,
            ::ClientboundUpdateDisplayNamePacket
        )
    }

    override val definition: PacketDefinition<out ByteBuf, out NetworkPacket>
        get() = UnityTranslateStandalonePackets.UPDATE_DISPLAY_NAME_CLIENT
}
