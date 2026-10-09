package xyz.bluspring.unitytranslate.shared.network.common.serverbound

import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs.optional
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.shared.network.SharedNetworkPacket
import java.util.*

@JvmRecord
data class UpdateTranscriptPacket(
    val transcript: String,
    val sourceLanguage: Language,
    val targetLanguage: Optional<Language>,
    val timeCreated: Long,
    val timeUpdated: Long,
) : SharedNetworkPacket {
    companion object {
        const val PACKET_ID = "update_transcript"

        val CODEC = CompositeCodecs.composite(
            NetworkCodecs.STRING_UTF8, UpdateTranscriptPacket::transcript,
            Language.NETWORK_CODEC, UpdateTranscriptPacket::sourceLanguage,
            Language.NETWORK_CODEC.optional(), UpdateTranscriptPacket::targetLanguage,
            NetworkCodecs.VAR_LONG, UpdateTranscriptPacket::timeCreated,
            NetworkCodecs.VAR_LONG, UpdateTranscriptPacket::timeUpdated,
            ::UpdateTranscriptPacket
        )
    }
}
