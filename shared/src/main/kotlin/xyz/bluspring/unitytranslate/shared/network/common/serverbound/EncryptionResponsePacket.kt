package xyz.bluspring.unitytranslate.shared.network.common.serverbound

import io.netty.buffer.ByteBuf
import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.PacketDefinition
import xyz.bluspring.unitytranslate.shared.network.common.UnityTranslateCommonPackets

@JvmRecord
data class EncryptionResponsePacket(
    val sharedSecret: ByteArray,
    val verifyBytes: ByteArray,
) : NetworkPacket {
    companion object {
        const val PACKET_ID = "encryption/response"

        @JvmField val CODEC = CompositeCodecs.composite(
            NetworkCodecs.BYTE_ARRAY, EncryptionResponsePacket::sharedSecret,
            NetworkCodecs.BYTE_ARRAY, EncryptionResponsePacket::verifyBytes,
            ::EncryptionResponsePacket
        )
    }

    override val definition: PacketDefinition<out ByteBuf, out NetworkPacket>
        get() = UnityTranslateCommonPackets.ENCRYPTION_RESPONSE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptionResponsePacket

        if (!sharedSecret.contentEquals(other.sharedSecret)) return false
        if (!verifyBytes.contentEquals(other.verifyBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sharedSecret.contentHashCode()
        result = 31 * result + verifyBytes.contentHashCode()
        return result
    }
}
