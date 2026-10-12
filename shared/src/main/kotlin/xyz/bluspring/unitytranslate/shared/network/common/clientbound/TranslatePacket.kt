package xyz.bluspring.unitytranslate.shared.network.common.clientbound

import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.shared.network.SharedNetworkPacket
import xyz.bluspring.unitytranslate.shared.network.common.serverbound.RequestTranslationPacket
import java.util.*

/**
 * A response for [RequestTranslationPacket].
 */
@JvmRecord
data class TranslatePacket(
    val requestId: UUID,
    val text: String,
    val sourceLanguage: Language,
    val targetLanguage: Language,
) : SharedNetworkPacket {
    companion object {
        const val PACKET_ID = "translate"

        val CODEC = CompositeCodecs.composite(
            NetworkCodecs.UUID, TranslatePacket::requestId,
            NetworkCodecs.STRING_UTF8, TranslatePacket::text,
            Language.NETWORK_CODEC, TranslatePacket::sourceLanguage,
            Language.NETWORK_CODEC, TranslatePacket::targetLanguage,
            ::TranslatePacket
        )
    }
}
