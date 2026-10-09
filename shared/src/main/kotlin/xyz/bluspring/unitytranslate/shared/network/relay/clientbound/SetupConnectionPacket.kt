package xyz.bluspring.unitytranslate.shared.network.relay.clientbound

import io.netty.buffer.ByteBuf
import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodec
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.unitytranslate.shared.network.AddressType
import xyz.bluspring.unitytranslate.shared.network.ConnectionType
import xyz.bluspring.unitytranslate.shared.network.SharedNetworkPacket

@JvmRecord
data class SetupConnectionPacket(
    val status: ConnectionStatus,
) : SharedNetworkPacket {
    companion object {
        const val PACKET_ID = "connection/setup"

        val CODEC = CompositeCodecs.composite(
            NetworkCodecs.STRING_UTF8.dispatch(ConnectionStatus::id) {
                when (it) {
                    "failed" -> ConnectionStatus.Failed.CODEC
                    "success" -> ConnectionStatus.Success.CODEC
                    else -> throw IllegalArgumentException("Unknown status type $it!")
                } as NetworkCodec<ByteBuf, ConnectionStatus> // why?
            }, SetupConnectionPacket::status,
            ::SetupConnectionPacket
        )
    }

    sealed interface ConnectionStatus {
        val id: String

        @JvmRecord
        data class Failed(
            val reason: String,
        ) : ConnectionStatus {
            override val id: String
                get() = "failed"

            companion object {
                val CODEC = CompositeCodecs.composite(
                    NetworkCodecs.STRING_UTF8, Failed::reason,
                    ::Failed
                )
            }
        }

        @JvmRecord
        data class Success(
            val address: String,
            val addressType: AddressType,
            val connectionType: ConnectionType,
        ) : ConnectionStatus {
            override val id: String
                get() = "success"

            companion object {
                val CODEC = CompositeCodecs.composite(
                    NetworkCodecs.STRING_UTF8, Success::address,
                    AddressType.CODEC, Success::addressType,
                    ConnectionType.CODEC, Success::connectionType,
                    ::Success
                )
            }
        }
    }
}
