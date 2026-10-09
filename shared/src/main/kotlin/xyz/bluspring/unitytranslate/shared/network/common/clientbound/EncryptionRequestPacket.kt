package xyz.bluspring.unitytranslate.shared.network.common.clientbound

import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.unitytranslate.shared.network.SharedNetworkPacket

@JvmRecord
data class EncryptionRequestPacket(
    val publicKey: ByteArray,
    val verifyBytes: ByteArray,
) : SharedNetworkPacket {
    companion object {
        const val PACKET_ID = "encryption/request"

        @JvmField val CODEC = CompositeCodecs.composite(
            NetworkCodecs.BYTE_ARRAY, EncryptionRequestPacket::publicKey,
            NetworkCodecs.BYTE_ARRAY, EncryptionRequestPacket::verifyBytes,
            ::EncryptionRequestPacket
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptionRequestPacket

        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!verifyBytes.contentEquals(other.verifyBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = publicKey.contentHashCode()
        result = 31 * result + verifyBytes.contentHashCode()
        return result
    }
}
