package xyz.bluspring.unitytranslate.network.common.serverbound

import io.netty.buffer.ByteBuf
import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs.setOf
import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.PacketDefinition
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.network.AdditionalNetworkCodecs
import xyz.bluspring.unitytranslate.network.UnityTranslateCommonPackets

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
    override val definition: PacketDefinition<out ByteBuf, out NetworkPacket>
        get() = UnityTranslateCommonPackets.UPDATE_LANGUAGES

    companion object {
        val CODEC = CompositeCodecs.composite(
            AdditionalNetworkCodecs.LANGUAGE.setOf(), UpdateLanguagesPacket::languages,
            AdditionalNetworkCodecs.LANGUAGE.setOf(), UpdateLanguagesPacket::selfTranslating,
            ::UpdateLanguagesPacket
        )
    }
}
