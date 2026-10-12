package xyz.bluspring.unitytranslate.shared.network.common.serverbound

import io.netty.buffer.ByteBuf
import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs.setOf
import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.PacketDefinition
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.shared.network.common.UnityTranslateCommonPackets

@JvmRecord
data class UpdateLanguagesPacket(
    /**
     * This should be all the languages that the client wants to be translated and receive.
     */
    val languages: Set<Language>,

    /**
     * This should be a set of languages that the client will translate by itself.
     */
    val selfTranslating: Set<Language>,
) : NetworkPacket {
    companion object {
        const val PACKET_ID = "update_languages"

        val CODEC = CompositeCodecs.composite(
            Language.NETWORK_CODEC.setOf(), UpdateLanguagesPacket::languages,
            Language.NETWORK_CODEC.setOf(), UpdateLanguagesPacket::selfTranslating,
            ::UpdateLanguagesPacket
        )
    }

    override val definition: PacketDefinition<out ByteBuf, out NetworkPacket>
        get() = UnityTranslateCommonPackets.UPDATE_LANGUAGES
}
