package xyz.bluspring.unitytranslate.shared.network.common.serverbound

import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs.setOf
import xyz.bluspring.unitytranslate.api.v2.Language
import xyz.bluspring.unitytranslate.shared.network.SharedNetworkPacket

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
) : SharedNetworkPacket {
    companion object {
        const val PACKET_ID = "update_languages"

        val CODEC = CompositeCodecs.composite(
            Language.NETWORK_CODEC.setOf(), UpdateLanguagesPacket::languages,
            Language.NETWORK_CODEC.setOf(), UpdateLanguagesPacket::selfTranslating,
            ::UpdateLanguagesPacket
        )
    }
}
