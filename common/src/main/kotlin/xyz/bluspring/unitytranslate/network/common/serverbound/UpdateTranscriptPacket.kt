package xyz.bluspring.unitytranslate.network.common.serverbound

import io.netty.buffer.ByteBuf
import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs.optional
import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.PacketDefinition
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.network.AdditionalNetworkCodecs
import xyz.bluspring.unitytranslate.network.UnityTranslateCommonPackets
import java.util.*

@JvmRecord
data class UpdateTranscriptPacket(
    val transcript: String,
    val sourceLanguage: Language,
    val targetLanguage: Optional<Language>,
    val timeCreated: Long,
    val timeUpdated: Long,
) : NetworkPacket {
    override val definition: PacketDefinition<out ByteBuf, out NetworkPacket>
        get() = UnityTranslateCommonPackets.UPDATE_TRANSCRIPT

    companion object {
        val CODEC = CompositeCodecs.composite(
            NetworkCodecs.STRING_UTF8, UpdateTranscriptPacket::transcript,
            AdditionalNetworkCodecs.LANGUAGE, UpdateTranscriptPacket::sourceLanguage,
            AdditionalNetworkCodecs.LANGUAGE.optional(), UpdateTranscriptPacket::targetLanguage,
            NetworkCodecs.VAR_LONG, UpdateTranscriptPacket::timeCreated,
            NetworkCodecs.VAR_LONG, UpdateTranscriptPacket::timeUpdated,
            ::UpdateTranscriptPacket
        )
    }
}
