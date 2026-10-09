package xyz.bluspring.unitytranslate.shared.network.common.serverbound

import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.unitytranslate.shared.network.SharedNetworkPacket

@JvmRecord
data class ClientInfoPacket(
    val protocolVersion: Int,

    /**
     * This should be the UnityTranslate version.
     */
    val clientVersion: String,

    /**
     * Platforms should be formatted like this:
     *  - Standalone
     *  - Minecraft 1.21.1, NeoForge 21.1.252
     *  - Minecraft 26.3, Fabric Loader 0.19.4
     *  - Hytale 0.6.8
     */
    val platform: String,

    /**
     * When in game mode, the Minecraft player's display name will always take precedence on the client, and this data is only
     * respected when the other client is in Standalone mode.
     */
    val username: String,
) : SharedNetworkPacket {
    companion object {
        const val PACKET_ID = "client_info"

        val CODEC = CompositeCodecs.composite(
            NetworkCodecs.VAR_INT, ClientInfoPacket::protocolVersion,
            NetworkCodecs.STRING_UTF8, ClientInfoPacket::clientVersion,
            NetworkCodecs.STRING_UTF8, ClientInfoPacket::platform,
            NetworkCodecs.STRING_UTF8, ClientInfoPacket::username,
            ::ClientInfoPacket
        )
    }
}
