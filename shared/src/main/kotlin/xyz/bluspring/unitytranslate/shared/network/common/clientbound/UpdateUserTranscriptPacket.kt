package xyz.bluspring.unitytranslate.shared.network.common.clientbound

import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.shared.network.SharedNetworkPacket
import java.util.*

@JvmRecord
data class UpdateUserTranscriptPacket(
    val source: UUID,
    val transcript: String,
    val sourceLanguage: Language,
    val targetLanguage: Language,
    val timeCreated: Long,
    val timeUpdated: Long,
) : SharedNetworkPacket {
    companion object {
        const val PACKET_ID = "update_user_transcript"

        val CODEC = CompositeCodecs.composite(
            NetworkCodecs.UUID, UpdateUserTranscriptPacket::source,
            NetworkCodecs.STRING_UTF8, UpdateUserTranscriptPacket::transcript,
            Language.NETWORK_CODEC, UpdateUserTranscriptPacket::sourceLanguage,
            Language.NETWORK_CODEC, UpdateUserTranscriptPacket::targetLanguage,
            NetworkCodecs.VAR_LONG, UpdateUserTranscriptPacket::timeCreated,
            NetworkCodecs.VAR_LONG, UpdateUserTranscriptPacket::timeUpdated,
            ::UpdateUserTranscriptPacket
        )
    }
}
