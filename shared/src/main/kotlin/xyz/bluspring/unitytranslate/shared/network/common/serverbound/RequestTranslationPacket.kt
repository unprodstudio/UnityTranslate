package xyz.bluspring.unitytranslate.shared.network.common.serverbound

import io.netty.buffer.ByteBuf
import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.PacketDefinition
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.shared.network.common.UnityTranslateCommonPackets
import java.util.*

/**
 * Used for requesting translations from the server, e.g. for signs or chat messages.
 */
@JvmRecord
data class RequestTranslationPacket(
    val requestId: UUID,
    val text: String,
    val sourceLanguage: Language,
    val targetLanguage: Language,
) : NetworkPacket {
    companion object {
        const val PACKET_ID = "request_translation"

        val CODEC = CompositeCodecs.composite(
            NetworkCodecs.UUID, RequestTranslationPacket::requestId,
            NetworkCodecs.STRING_UTF8, RequestTranslationPacket::text,
            Language.NETWORK_CODEC, RequestTranslationPacket::sourceLanguage,
            Language.NETWORK_CODEC, RequestTranslationPacket::targetLanguage,
            ::RequestTranslationPacket
        )
    }

    override val definition: PacketDefinition<out ByteBuf, out NetworkPacket>
        get() = UnityTranslateCommonPackets.REQUEST_TRANSLATION
}
