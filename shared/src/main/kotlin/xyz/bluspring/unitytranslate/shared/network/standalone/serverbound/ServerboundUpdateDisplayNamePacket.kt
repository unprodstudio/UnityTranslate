package xyz.bluspring.unitytranslate.shared.network.standalone.serverbound

import io.netty.buffer.ByteBuf
import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.PacketDefinition
import xyz.bluspring.unitytranslate.shared.network.standalone.UnityTranslateStandalonePackets

@JvmRecord
data class ServerboundUpdateDisplayNamePacket(
    val name: String,
) : NetworkPacket {
    companion object {
        const val PACKET_ID = "update_display_name/serverbound"

        @JvmField val CODEC = CompositeCodecs.composite(
            NetworkCodecs.STRING_UTF8, ServerboundUpdateDisplayNamePacket::name,
            ::ServerboundUpdateDisplayNamePacket
        )
    }

    override val definition: PacketDefinition<out ByteBuf, out NetworkPacket>
        get() = UnityTranslateStandalonePackets.UPDATE_DISPLAY_NAME_SERVER
}
