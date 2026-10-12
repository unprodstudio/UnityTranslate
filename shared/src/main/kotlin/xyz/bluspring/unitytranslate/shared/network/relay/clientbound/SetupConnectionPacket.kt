package xyz.bluspring.unitytranslate.shared.network.relay.clientbound

import io.netty.buffer.ByteBuf
import xyz.bluspring.modernnetworking.api.v2.codec.CompositeCodecs
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodec
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodecs
import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.PacketDefinition
import xyz.bluspring.unitytranslate.shared.network.AddressType
import xyz.bluspring.unitytranslate.shared.network.ConnectionType
import xyz.bluspring.unitytranslate.shared.network.relay.UnityTranslateRelayPackets

@JvmRecord
data class SetupConnectionPacket(
    val status: ConnectionStatus,
) : NetworkPacket {
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

    override val definition: PacketDefinition<out ByteBuf, out NetworkPacket>
        get() = UnityTranslateRelayPackets.SETUP_CONNECTION

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
