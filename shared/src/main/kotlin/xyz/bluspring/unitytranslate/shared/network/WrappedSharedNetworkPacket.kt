package xyz.bluspring.unitytranslate.shared.network

import io.netty.buffer.ByteBuf
import xyz.bluspring.modernnetworking.api.v2.codec.NetworkCodec
import xyz.bluspring.modernnetworking.api.v2.packet.NetworkPacket
import xyz.bluspring.modernnetworking.api.v2.packet.PacketDefinition
import xyz.bluspring.modernnetworking.api.v2.packet.registry.NamespacedPacketRegistry
import xyz.bluspring.modernnetworking.api.v2.packet.registry.PacketRegistry

@JvmRecord
data class WrappedSharedNetworkPacket<T : SharedNetworkPacket>(
    override val definition: PacketDefinition<out ByteBuf, out NetworkPacket>,
    val wrapped: T,
) : NetworkPacket {
    companion object {
        @JvmStatic
        fun <B : ByteBuf, T : SharedNetworkPacket> PacketRegistry.register(namespace: String, id: String, codec: NetworkCodec<B, T>): PacketDefinition<B, WrappedSharedNetworkPacket<T>> {
            lateinit var definition: PacketDefinition<B, WrappedSharedNetworkPacket<T>>
            definition = this.register(namespace, id, codec.xmap({ WrappedSharedNetworkPacket(definition, it) }, { it.wrapped }))
            return definition
        }

        @JvmStatic
        fun <B : ByteBuf, T : SharedNetworkPacket> NamespacedPacketRegistry.register(id: String, codec: NetworkCodec<B, T>): PacketDefinition<B, WrappedSharedNetworkPacket<T>> {
            lateinit var definition: PacketDefinition<B, WrappedSharedNetworkPacket<T>>
            definition = this.register(id, codec.xmap({ WrappedSharedNetworkPacket(definition, it) }, { it.wrapped }))
            return definition
        }
    }
}
