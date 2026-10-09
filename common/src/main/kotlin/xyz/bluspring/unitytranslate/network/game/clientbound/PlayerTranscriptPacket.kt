package xyz.bluspring.unitytranslate.network.game.clientbound

import io.netty.buffer.ByteBuf
import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.PacketDefinition
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.network.UnityTranslateGamePackets
import java.util.*

@JvmRecord
data class PlayerTranscriptPacket(
    val sender: UUID,
    val text: String,
    val sourceLanguage: Language,
    val translatedLanguage: Language,
) : NetworkPacket {
    override val definition: PacketDefinition<out ByteBuf, out NetworkPacket>
        get() = UnityTranslateGamePackets.PLAYER_TRANSCRIPT

    companion object {
        val CODEC = CompositeCodecs.composite(
            NetworkCodecs.UUID, PlayerTranscriptPacket::sender,
            NetworkCodecs.STRING_UTF8, PlayerTranscriptPacket::text,
            AdditionalNetworkCodecs.LANGUAGE, PlayerTranscriptPacket::sourceLanguage,
            AdditionalNetworkCodecs.LANGUAGE, PlayerTranscriptPacket::translatedLanguage,
            ::PlayerTranscriptPacket
        )
    }
}
